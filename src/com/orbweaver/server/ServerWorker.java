package com.orbweaver.server;

import com.google.gson.*;
import com.orbweaver.commons.*;

import java.io.*;
import java.net.Socket;

import static com.orbweaver.commons.Constants.CODE_MESSAGE_WHO_IS_SCHEDULER;

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
                scheduler.setAddress(this.server.geteCoordinatorAddress());
                scheduler.setPort(this.server.getCoordinatorPort());
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

                this.server.getRequests().add(newRequestMsg.getRequestInfo());
                break;

            // Mensaje de difusión recibido : Actualizar estado de request
            case Constants.CODE_MESSAGE_UPDATE_REQUEST:
                // Actualización de una request modo actualizar backup
                RequestUpdateRequestMsg requestUpdate = gson.fromJson(content, RequestUpdateRequestMsg.class);

                RequestInfo request = this.server.getRequestByID(requestUpdate.getRequestID());
                request.setStatus(requestUpdate.getNewStatus());
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
                RequestAnswerMsg updateRequest = updateStatusRequestScheduler(
                        requestServiceMsg.getIdRequest(),
                        RequestInfo.StatusRequest.RUNNING
                );

                // Establece el estado de la request en mi lista
                myrequest.setStatus(RequestInfo.StatusRequest.RUNNING);

                // Enviamos el mensaje de respuesta -- errror -success
                content = gson.toJson(updateRequest);
                try {
                    dataOutputStream.writeUTF(content);
                } catch (IOException e) {
                    throw new RuntimeException(
                            String.format("Error: Cannot write JSON to Server ( %s , %d)",
                                    clientSocket.getInetAddress().getHostName(),clientSocket.getPort())
                            , e);
                }
                // En caso de que sea exitosa la petición, llamamos a la clase encargada de manejarla con el socket
                // del cliente
                if(updateRequest.isSuccess()) {
                    ServiceInterfaz service = server.getServiceExec(requestServiceMsg.getName());

                    service.handleClient(clientSocket, dataInputStream, dataOutputStream);

                    updateStatusRequestScheduler(requestServiceMsg.getIdRequest(), RequestInfo.StatusRequest.DONE);
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

    /**
     * Se comunica con el scheduler para actualizar el estado de una request
     * Scheduler responde Error:
     *  - Si la request es inválida
     *  - Si la request ya está resuelta
     *  Sino:
     *  - Si la request es válida , establece el servidor como ejecutandola
     */
    private RequestAnswerMsg updateStatusRequestScheduler(String idRequest, RequestInfo.StatusRequest newStatus) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Socket socket;

        try {
            socket = new Socket(this.server.geteCoordinatorAddress(),this.server.getCoordinatorPort());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Scheduler ( %s , %d)",
                            this.server.geteCoordinatorAddress(), this.server.getCoordinatorPort()), e);
        }

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;


        // Obtenemos el contenido del mensaje del cliente
        try{
            dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Scheduler ( %s , %d)",
                            this.server.geteCoordinatorAddress(), this.server.getCoordinatorPort()), e);
        }

        RequestUpdateRequestMsg requestUpdateRequestMsg = new RequestUpdateRequestMsg(
                this.server.getId(),
                idRequest,
                newStatus
        );
        content = gson.toJson(requestUpdateRequestMsg);
        System.out.println("[Server] Updating request to Scheduler : " + content);

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort())
                    , e);
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Scheduler ( %s , %d)",
                            this.server.geteCoordinatorAddress(), this.server.getCoordinatorPort()), e);
        }

        System.out.println("[Server] Updating request to Scheduler answer : " + content);
        RequestAnswerMsg answer = gson.fromJson(content, RequestAnswerMsg.class);

        try{
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }

        return answer;

    }

}