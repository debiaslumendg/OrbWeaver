package com.orbweaver.scheduler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;

public class SchedulerWorker implements Runnable{

    private Socket clientSocket;
    private SchedulerServer scheduler;
    public SchedulerWorker(Socket clientSocket , SchedulerServer scheduler) {
        this.clientSocket = clientSocket;
        this.scheduler = scheduler;
    }

    public void run() {
        try {

            //MyType my_object = gson.fromJson(jsonSource, MyType.class)



            InputStream input  = clientSocket.getInputStream();
            Gson gson = new Gson();


            OutputStream output = clientSocket.getOutputStream();
            long time = System.currentTimeMillis();
            output.write(("HTTP/1.1 200 OK\n\nWorkerRunnable:").getBytes());
            output.close();
            input.close();
            System.out.println("Request processed: " + time);
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }
}