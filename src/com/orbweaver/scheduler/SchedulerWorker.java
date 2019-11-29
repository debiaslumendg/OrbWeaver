package com.orbweaver.scheduler;

import com.google.gson.*;
import com.google.gson.stream.MalformedJsonException;
import com.orbweaver.commons.Constants;
import com.orbweaver.commons.RequestAddServerAnswerMsg;
import com.orbweaver.commons.RequestAddServerMsg;
import com.orbweaver.commons.Util;

import java.io.*;
import java.net.Socket;

public class SchedulerWorker implements Runnable{

    private Socket clientSocket;
    private Scheduler scheduler;


    public SchedulerWorker(Socket clientSocket , Scheduler scheduler) {
        this.clientSocket = clientSocket;
        this.scheduler = scheduler;
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
                    String.format("Error: Cannot open connection to Server ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }


        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot read JSON from Server ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }


        // Parseamos el mensaje a JSON
        try {
            jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();
        }catch (JsonSyntaxException e){
            System.out.println("Error : incorrect message sent by client");
            System.out.println("Message: " + content);

            return ;
        }

        System.out.println(jsonObjectMessage);

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
        }

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Server ( %s , %d)",
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