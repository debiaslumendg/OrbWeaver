package com.orbweaver.server;

import com.google.gson.*;
import com.orbweaver.commons.*;

import java.io.*;
import java.net.Socket;

import static com.orbweaver.commons.Constants.*;

public class ServerWorker implements Runnable{

    protected Socket clientSocket = null;
    protected Server server   = null;

    public ServerWorker(Server server,Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.server   = server;
    }

    public void run() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;
        JsonObject jsonObjectMessage;

        // Obtenemos el contenido del mensaje del cliente
        try{
            dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot open connection to Client ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostAddress(),this.clientSocket.getPort())
                    , e);
        }


        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Client ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }


        // Parseamos el mensaje a JSON
        try {
            jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();
        }catch (JsonSyntaxException e){
            System.out.println("[Server] Error : incorrect message sent by client");
            System.out.println("[Server] Message: " + content);

            return ;
        }

        System.out.println("[Server] " + jsonObjectMessage);

        int code = jsonObjectMessage.get("code").getAsInt();

        switch (code){

            // Mensaje preguntando quién es el Scheduler
            case CODE_MESSAGE_WHO_IS_SCHEDULER:
                ServerInfo scheduler = new ServerInfo();
                scheduler.setAddress(this.server.getCoordinatorAddress());
                scheduler.setPort(this.server.getCoordinatorPort());

                if(!this.server.pingServer(scheduler)){
                    this.server.startEleccion();
                }
                // Enviamos el mensaje con el scheduler
                content = gson.toJson(scheduler);
                try {
                    dataOutputStream.writeUTF(content);
                } catch (IOException e) {
                    System.out.format("Error: Cannot write JSON to Server ( %s , %d)\n",
                            clientSocket.getInetAddress().getHostName(),clientSocket.getPort());
                }
                break;
            // Mensaje de difusión recibido : Agregar servidor
            case Constants.CODE_MESSAGE_ADD_SERVER:
                RequestAddServerMsg requestAddServerMsg = gson.fromJson(content, RequestAddServerMsg.class);
                server.getServers().add(requestAddServerMsg.getServer());
                this.server.setNextServerID(server.getNextServerID() + 1);
                break;
            // Mensaje de difusión recibido :Eliminar servidor
            case Constants.CODE_MESSAGE_REMOVE_SERVER:
                for(JsonElement server_id : jsonObjectMessage.get("id_servers").getAsJsonArray()) {
                    this.server.removeServerByID(server_id.getAsInt());
                }
                break;
            // Mensaje de difusión recibido : Request creada
            case Constants.CODE_MESSAGE_NEW_REQUEST:
                RequestNewRequestMsg newRequestMsg = gson.fromJson(content, RequestNewRequestMsg.class);

                this.server.addRequest(newRequestMsg.getRequestInfo());
                break;

            // Mensaje de difusión recibido : Actualizar estado de request
            case Constants.CODE_MESSAGE_UPDATE_REQUEST:
                // Actualización de una request modo actualizar backup
                RequestUpdateRequestMsg requestUpdate = gson.fromJson(content, RequestUpdateRequestMsg.class);

                RequestInfo request = this.server.getRequestByID(requestUpdate.getRequestID());
                request.setStatus(requestUpdate.getNewStatus());
                break;

            case Constants.CODE_MESSAGE_ELECCION:
                if(this.server.isScheduler()){
                    ServerInfo serverInfo = new ServerInfo();
                    serverInfo.setAddress(this.server.getCoordinatorAddress());
                    serverInfo.setPort(this.server.getCoordinatorPort());

                    content = String.format("{code:%d,scheduler:%s}",CODE_MESSAGE_COORDINATOR,
                            new Gson().toJson(serverInfo));
                    try {
                        dataOutputStream.writeUTF(content);
                    } catch (IOException e) {
                        System.out.format("Error: Cannot write JSON to ( %s , %d)\n",
                                clientSocket.getInetAddress().getHostName(),clientSocket.getPort());
                    }
                }else{
                    this.server.startEleccion();
                }
                break;
            case CODE_MESSAGE_COORDINATOR:
                ServerInfo serverInfo = gson.fromJson(jsonObjectMessage.get("scheduler"),ServerInfo.class);
                this.server.setScheduler(serverInfo);
                this.server.setEleccionStarted(false);
                this.server.triggerSavedRequestUpdates();
                break;
            // Mensaje recibido: "Ping" . ¿Are you alive?
            case Constants.CODE_MESSAGE_PING:
                // Los pings no se responden
                break;

            // Mensaje de ejecución de servicio por cliente
            case Constants.CODE_MESSAGE_EXEC_SERVICE:

                // Obtenemos una clase para el mensaje
                RequestServiceMsg requestServiceMsg = gson.fromJson(content, RequestServiceMsg.class);

                // Obtenemos la request que tenemos guardada en nuestra lista
                // La que se debió guardar, por un mensaje de difusión CODE_REQUEST_UPDATE_REQUEST que envió el Scheduler
                RequestInfo myrequest = this.server.getRequestByID(requestServiceMsg.getIdRequest());

                // Nos comunicamos con el scheduler para actualizar el estado de la peticion
                // El scheduler hace la difusión del cambio de estado de la request a RUNNING a todos los demás miembros
                // del grupo
                RequestAnswerMsg updateRequest = this.server.updateStatusRequestScheduler(
                        requestServiceMsg.getIdRequest(),
                        RequestInfo.StatusRequest.RUNNING
                );

                if(updateRequest == null) {
                    this.server.startEleccion();
                    updateRequest = new RequestAnswerMsg();
                    updateRequest.setCode(Constants.CODE_ERROR_SCHEDULER_NOT_AVAILABLE);
                    updateRequest.setStatus(STATUS_ERROR_REQUEST);
                }

                if(updateRequest.isSuccess() && this.server.isScheduler()) {
                    // Establece el estado de la request en mi lista
                    myrequest.setStatus(RequestInfo.StatusRequest.RUNNING);
                }

                // Enviamos el mensaje de respuesta -- errror -success al Cliente
                content = gson.toJson(updateRequest);
                try {
                    dataOutputStream.writeUTF(content);
                } catch (IOException e) {
                    throw new RuntimeException(
                            String.format("Error: Cannot write JSON to Client ( %s , %d)",
                                    clientSocket.getInetAddress().getHostName(),clientSocket.getPort())
                            , e);
                }
                // En caso de que sea exitosa la petición, llamamos a la clase encargada de manejarla con el socket
                // del cliente
                if(updateRequest.isSuccess()) {
                    ServiceInterfaz service = server.getServiceExec(requestServiceMsg.getName());

                    service.handleClient(clientSocket, dataInputStream, dataOutputStream);

                    updateRequest = this.server.updateStatusRequestScheduler(requestServiceMsg.getIdRequest(), RequestInfo.StatusRequest.DONE);
                    if(updateRequest == null) {
                        this.server.startEleccion();
                        this.server.addRequestIdToCompleteWhenSchedulerAvailable(requestServiceMsg.getIdRequest());
                    }else if(updateRequest.isSuccess()&& this.server.isScheduler())
                        myrequest.setStatus(RequestInfo.StatusRequest.DONE);
                }
                break;

        }

        try{
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}