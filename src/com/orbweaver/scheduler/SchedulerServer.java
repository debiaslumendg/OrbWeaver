package com.orbweaver.scheduler;

import com.google.gson.JsonArray;
import com.orbweaver.scheduler.tables.RequestServer;
import com.orbweaver.scheduler.tables.ServicioInfo;
import com.orbweaver.scheduler.tables.ServidorInfo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class SchedulerServer implements Runnable {

    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;

    protected HashMap<Integer, ServicioInfo> mServicios = new HashMap<>();
    protected HashMap<Integer, ServidorInfo> mServidores = new HashMap<>();
    protected HashMap<Integer, RequestServer> mRequestServer = new HashMap<>() ;
    // protected HashMap<Integer, RequestServer> mRequestServer = new HashMap<>() ;

    public SchedulerServer(int port){
        this.serverPort = port;
    }

    public void readFileJsonServices(){

        JsonArray jsonArrayServices = null;
        FileReader br;
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get("services.json"));
            String content = new String(bytes);
            jsonArrayServices = new JsonParser().parse(content).getAsJsonArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(jsonArrayServices != null){
            String nameService;
            for (int i = 0; i < jsonArrayServices.size(); i++) {
                nameService = jsonArrayServices.get(i).getAsJsonObject().get("name").getAsString();
                mServicios.put(0,new ServicioInfo(0,nameService)) ;
            }
        }
    }
    public void run(){

        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        readFileJsonServices();
        openServerSocket();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            new Thread(new SchedulerWorker(clientSocket,this)).start();
        }
        System.out.println("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }

}