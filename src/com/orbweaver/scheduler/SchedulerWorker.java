package com.orbweaver.scheduler;

import com.google.gson.*;
import com.google.gson.stream.MalformedJsonException;
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
        try {

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            InputStream input  = clientSocket.getInputStream();

            // Obtenemos el contenido del mensaje del cliente
            String content = Util.convertInputStreamToString(input);

            input.close();

            // Parseamos el mensaje a JSON
            JsonObject jsonObjectMessage;
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
                case 1:
                    RequestAddServerMsg requestAddServerMsg = gson.fromJson(content, RequestAddServerMsg.class);
                    scheduler.addServer(requestAddServerMsg.getServer());
                break;
            }


            long time = System.currentTimeMillis();
            System.out.println("Request processed: " + time);

        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }
}