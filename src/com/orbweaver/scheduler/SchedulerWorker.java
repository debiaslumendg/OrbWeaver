package com.orbweaver.scheduler;

import com.google.gson.*;
import com.orbweaver.commons.*;

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

        System.out.println("[Scheduler] " + jsonObjectMessage);

        // Obtenemos el código, esto nos dice de qué va el mensaje.
        int code = jsonObjectMessage.get("code").getAsInt();

        switch (code){
            case Constants.CODE_REQUEST_ADD_SERVER:
                RequestAddServerMsg requestAddServerMsg = gson.fromJson(content, RequestAddServerMsg.class);

                int newServerID = scheduler.addServer(requestAddServerMsg.getServer());

                RequestAddServerAnswerMsg requestAddServerAnswerMsg = new RequestAddServerAnswerMsg(
                        newServerID,
                        scheduler.getServices(),
                        scheduler.getServers()
                );

                content = gson.toJson(requestAddServerAnswerMsg);

                break;
            case Constants.CODE_REQUEST_EXEC_SERVICE:
                // Cliente quiere ejecutar un servicio
                RequestServiceMsg requestServiceMsg = gson.fromJson(content, RequestServiceMsg.class);

                RequestServiceAnswerSuccessMsg requestServiceAnswerSuccessMsg = scheduler.createRequestService(requestServiceMsg.getName());

                content = gson.toJson(requestServiceAnswerSuccessMsg);

                break;
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