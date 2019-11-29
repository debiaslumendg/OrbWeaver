package com.orbweaver.server;

import com.google.gson.*;
import com.orbweaver.commons.Constants;
import com.orbweaver.commons.RequestAddServerAnswerMsg;
import com.orbweaver.commons.RequestAddServerMsg;

import java.io.*;
import java.net.Socket;

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

        System.out.println("[Scheduler] " + jsonObjectMessage);

        int code = jsonObjectMessage.get("code").getAsInt();

        switch (code){
            case Constants.CODE_REQUEST_ADD_SERVER:
                RequestAddServerMsg requestAddServerMsg = gson.fromJson(content, RequestAddServerMsg.class);

                server.addServer(requestAddServerMsg.getServer());

                break;
        }

        /*try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Server ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }*/

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