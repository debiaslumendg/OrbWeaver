package com.orbweaver.server;

import com.google.gson.*;
import com.orbweaver.commons.*;

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

        System.out.println("[Server] " + jsonObjectMessage);

        int code = jsonObjectMessage.get("code").getAsInt();

        switch (code){
            case Constants.CODE_REQUEST_ADD_SERVER:
                RequestAddServerMsg requestAddServerMsg = gson.fromJson(content, RequestAddServerMsg.class);

                server.addServer(requestAddServerMsg.getServer());

                break;
            case Constants.CODE_REQUEST_EXEC_SERVICE:
                RequestServiceMsg requestServiceMsg = gson.fromJson(content, RequestServiceMsg.class);

                if(isValidRequestID(requestServiceMsg.getIdRequest())){
                    ServiceInterfaz service = server.getServiceExec(requestServiceMsg.getName());
                    service.handleClient(clientSocket,dataInputStream,dataOutputStream);
                }

                break;
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


    private boolean isValidRequestID(String idRequest) {
        return true;
    }
}