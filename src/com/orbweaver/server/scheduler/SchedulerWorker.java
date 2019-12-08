package com.orbweaver.server.scheduler;

import com.google.gson.*;
import com.orbweaver.commons.*;
import com.orbweaver.server.RequestInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * Hilo que atiende los mensajes enviados al Scheduler.
 *
 * Atiende :
 * 		Solicitudes de requerimientos de servicios.
 * 		Solicitudes de agregar/eliminar del grupo de servidores.
 * 		y ..
 *
 *
 *  Autores:
 *      Natascha Gamboa      12-11250
 * 	    Manuel  Gonzalez    11-10390
 * 	    Pedro   Perez       10-10574
 */
public class SchedulerWorker implements Runnable {

    /**Socket que conecta al cliente*/
    private Socket clientSocket;

    /**Apuntador a la referencia del scheduler*/
    private Scheduler scheduler;


    /**
     * Crea el Scheduler
     * @param clientSocket
     * @param scheduler
     */
    public SchedulerWorker(Socket clientSocket , Scheduler scheduler) {
        this.clientSocket = clientSocket;
        this.scheduler = scheduler;
    }

    /**
     * Ejecuta el código del scheduler para este hilo.
     *
     * Lee el mensaje del cliente y envia una respuesta
     */
    public void run() {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;
        JsonObject jsonObjectMessage;

        // Entrada y Salida con el cliente
        try{
            dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Scheduler] Error: Cannot open connection to Client ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }


        // Leemos el mensaje
        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Scheduler] Error: Cannot read JSON from Client ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }


        // Parseamos el mensaje a JSON
        try {
            jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();
        }catch (JsonSyntaxException e){
            System.out.println("[Scheduler] Error : incorrect message sent by client");
            System.out.println("[Scheduler] Message: " + content);

            return ;
        }

        System.out.println("[Scheduler] Received " + jsonObjectMessage);

        // Obtenemos el código, esto nos dice de qué va el mensaje.
        int code = jsonObjectMessage.get("code").getAsInt();

        ServerInfo serverInfo;
        switch (code){
            case Constants.CODE_MESSAGE_ADD_SERVER:
                RequestAddServerMsg requestAddServerMsg = gson.fromJson(content, RequestAddServerMsg.class);

                serverInfo = requestAddServerMsg.getServer();
                serverInfo.setAddress(clientSocket.getInetAddress().getHostAddress());

                int newServerID = scheduler.addServer(requestAddServerMsg);

                RequestAddServerAnswerMsg requestAddServerAnswerMsg = new RequestAddServerAnswerMsg(
                        newServerID,
                        scheduler.getServers(),
                        scheduler.getRequests()
                );

                content = gson.toJson(requestAddServerAnswerMsg);

                break;

            case Constants.CODE_MESSAGE_GET_MEMBERS_LIST:

                content = gson.toJson(this.scheduler.getServers());
                break;
            case Constants.CODE_MESSAGE_EXEC_SERVICE:
                // Cliente quiere ejecutar un servicio
                RequestServiceMsg requestServiceMsg = gson.fromJson(content, RequestServiceMsg.class);

                if(StringUtils.isNotEmpty(requestServiceMsg.getIdRequest())){

                    RequestInfo requestInfo = this.scheduler.getRequestByID(requestServiceMsg.getIdRequest());

                    serverInfo = this.scheduler.getServerByID(requestInfo.getIdServer());

                    if(serverInfo != null && !this.scheduler.pingServer(serverInfo)){
                        this.scheduler.removeServerByID(requestInfo.getIdServer());
                        this.scheduler.sendMessageRemoveServerToGroup(requestInfo.getIdServer());
                    }

                }

                RequestServiceAnswerMsg requestServiceAnswerMsg = scheduler.createRequestService(requestServiceMsg.getName());

                if(requestServiceAnswerMsg.isSuccess()){

                    // Si la request se creo correctamente actualizamos todos los servidores creandole la request
                    RequestInfo requestInfo = this.scheduler.getRequestByID(requestServiceAnswerMsg.getRequestId());
                    RequestNewRequestMsg requestNewRequestMsg = new RequestNewRequestMsg(requestInfo);
                    this.scheduler.sendMessageToGroup(new Gson().toJson(requestNewRequestMsg));
                }

                content = gson.toJson(requestServiceAnswerMsg);

                break;
            case Constants.CODE_MESSAGE_UPDATE_REQUEST:
                // Servidor quiere actualizar el estado de una request
                RequestUpdateRequestMsg requestUpdate = gson.fromJson(content, RequestUpdateRequestMsg.class);

                RequestAnswerMsg answer = this.scheduler.updateRequest(requestUpdate);

                content = gson.toJson(answer);

        }

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Client ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }

        //long time = System.currentTimeMillis();
        //System.out.println("Request processed: " + time);

        try{
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }

}