package com.orbweaver.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.orbweaver.commons.*;
import com.orbweaver.scheduler.Scheduler;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import static com.orbweaver.commons.Util.*;

public class Server {

    private int          serverPort     = Constants.DEFAULT_SERVER_PORT;

    private ServerSocket serverSocket   = null;

    private boolean iAmScheduler = false;

    private WordCountService wordCountService = new WordCountService();

    private String coordinatorAddress     = "";
    private int coordinatorPort               = Constants.DEFAULT_SCHEDULER_PORT;;

    private int schedulerPort           = Constants.DEFAULT_SCHEDULER_PORT;

    private ArrayList<ServiceInfo> services = new ArrayList<ServiceInfo>();
    private ArrayList<ServerInfo> servers = new ArrayList<>();
    private ServerInfo myServerInfo;

    private boolean      isStopped      = false;
    private int myId;

    public Server(int serverPort,String coordinatorAddress, int coordinatorPort,int schedulerPort,boolean isCheduler){
        this.serverPort = serverPort;
        this.coordinatorAddress = coordinatorAddress;
        this.coordinatorPort = coordinatorPort;
        this.schedulerPort = schedulerPort;

        services.add(new ServiceInfo("wordcount"));

        myServerInfo = new ServerInfo("manuggz","127.0.0.1",this.serverPort, services);

        servers.add(myServerInfo);
        iAmScheduler = isCheduler;
    }

    private void addServerToGroup(){
        System.out.format("[Server] Connecting to Scheduler(%s,%d) to add me to the Group\n",
                this.coordinatorAddress,this.coordinatorPort);

        Socket socketScheduler = null;
        try {
            socketScheduler = new Socket(this.coordinatorAddress,this.coordinatorPort);
            System.out.println("[Server] Connected to the Scheduler");
        } catch (IOException e) {
            // TODO: Puede time out
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot connect to Scheduler ( %s , %d)",
                            this.coordinatorAddress,this.coordinatorPort)
                    , e);
        }


        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        JsonObject jsonObjectMessage;
        String json;

        try {
            dataOutputStream = new DataOutputStream(socketScheduler.getOutputStream());
            dataInputStream = new DataInputStream(new BufferedInputStream(socketScheduler.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot open connection to Scheduler ( %s , %d)",
                            this.coordinatorAddress,this.coordinatorPort)
                    , e);
        }

        RequestAddServerMsg requestAddServerMsg = new RequestAddServerMsg(myServerInfo);
        json = gson.toJson(requestAddServerMsg);
        System.out.println("[Server] Sending " + json + " to the Scheduler");

        try {
            dataOutputStream.writeUTF(json);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Scheduler ( %s , %d)",
                            this.coordinatorAddress,this.coordinatorPort)
                    , e);
        }


        try {

            // Obtenemos el contenido del mensaje del scheduler
            json = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot read answer from Scheduler ( %s , %d)",
                            this.coordinatorAddress,this.coordinatorPort)
                    , e);
        }

        // Parseamos el mensaje a JSON
        try {
            jsonObjectMessage = new JsonParser().parse(json).getAsJsonObject();
        }catch (JsonSyntaxException e){
            System.out.println("Error : incorrect message sent by Scheduler");
            System.out.println("Message: " + json);
            throw new RuntimeException(
                    String.format("Error: incorrect message sent by Scheduler ( %s , %d)",
                            this.coordinatorAddress,this.coordinatorPort)
                    , e);

        }

        System.out.println(jsonObjectMessage);

        int status = jsonObjectMessage.get("status").getAsInt();

        switch (status){
            case Constants.STATUS_SUCCESS_REQUEST:
                RequestAddServerAnswerMsg requestAddServerAnswerMsg = gson.fromJson(json, RequestAddServerAnswerMsg.class);
                System.out.println("[Server] Got from Scheduler: ");
                services = requestAddServerAnswerMsg.getServices();
                System.out.println("[Server] \tServices: " + services);
                servers = requestAddServerAnswerMsg.getServers();
                System.out.println("[Server] \tServers: " + servers);

                myId = requestAddServerAnswerMsg.getServer_id();

                myServerInfo = getServerByID(myId);

                System.out.println("[Server] \tMy ID: " + myId);
                System.out.println("[Server] Added to the Group...");

                break;
        }


        try {
            dataOutputStream.close();
            dataInputStream.close();
            socketScheduler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ArrayList<ServiceInfo> getServices(){
        return services;
    }

    private int getServiceIDByName(String name){
        for(ServiceInfo serviceInfo : services){
            if(serviceInfo.getName().equals(name)) {
                return serviceInfo.getId();
            }
        }

        return -1;
    }

    private ServerInfo getServerByID(int id){
        for(ServerInfo serverInfo : servers){
            if(serverInfo.getId() == id) {
                return serverInfo;
            }
        }

        return null;

    }

    private void sendJsonToServer(String json , ServerInfo serverInfo){

        Socket socket;

        try {
            socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }


        DataOutputStream dataOutputStream;
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot open connection to Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }

        try {
            dataOutputStream.writeUTF(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Envia un mensaje al grupo informandoles del recien llegado
     * TODO: Hacer ordenamiento de los mensajes agregar / eliminar servidor
     * @param newServerInfo
     */
    private void sendUpdateNewServer(ServerInfo newServerInfo) {

        Gson gson = new Gson();

        RequestAddServerMsg requestAddServerMsg = new RequestAddServerMsg(newServerInfo);
        String json = gson.toJson(requestAddServerMsg);

        for(ServerInfo serverInfo : servers) {
            if(serverInfo.getId() != myServerInfo.getId()) {
                sendJsonToServer(json, serverInfo);
            }
        }

    }

    public synchronized int addServer(ServerInfo serverInfo){

        // Se lee el id del anterior servidor para controlar la eliminación y agregación de servidores
        int newServerID;
        if(iAmScheduler) {
            newServerID = servers.get(servers.size() - 1).getId() + 1;
            serverInfo.setId(newServerID);
        }else{
            newServerID = serverInfo.getId();
        }

        for(ServiceInfo serviceInfo : serverInfo.getServices()){

            int newServiceID = getServiceIDByName(serviceInfo.getName());

            if(newServiceID == -1) {
                if(iAmScheduler) {
                    newServiceID = services.get(services.size() - 1).getId() + 1;
                }
                services.add(serviceInfo);
            }

            if(iAmScheduler) {
                serviceInfo.setId(newServiceID);
            }
        }

        if(iAmScheduler) {
            sendUpdateNewServer(serverInfo);
        }

        servers.add(serverInfo);

        System.out.format("Added server ID=%d :\n\tAddress - (%s,%d)\n", newServerID, serverInfo.getAddress(), serverInfo.getPort());
        System.out.format("\tServices %s\n", serverInfo.getServices());

        return newServerID;
    }

    public ArrayList<ServerInfo> getServers(){
        return servers;
    }

    public void run(){

        if(!iAmScheduler) {
            addServerToGroup();
        }else{
            myServerInfo.setId(0);
            for(int i = 0 ; i < services.size() ; i++){
                services.get(i).setId(i);
            }
            new Thread(new Scheduler(this,schedulerPort)).start();
        }

        openServerSocket();

        while(! isStopped()){
            Socket clientSocket = null;
            System.out.println("[Server] Waiting for a client ...");
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("[Server] Server Stopped.") ;
                    return;
                }
                throw new RuntimeException("[Server] Error accepting client connection", e);
            }

            InetAddress inetAddress = clientSocket.getInetAddress();
            System.out.format("[Server] Accepted client %s \n",inetAddress.getHostAddress());
            new Thread(new ServerWorker(this,clientSocket)).start();
        }

        stop();
        System.out.println("[Server]Server Stopped.") ;
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
            System.out.format("[Server] Running server on port: %d\n",this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }

    /**
     * En caso de que los argumentos no hayan sido provistos como argumentos
     * Le pregunta al usuario la dirección y puerto del coordinador
     * @param portCoordinator
     * @return
     */
    private static String[] askUserSchedulerAddress(int portCoordinator) {

        String[] values = new String[]{"", String.valueOf(portCoordinator)};
        Scanner scanner = new Scanner(System. in);

        System.out.println("Please, Insert the Scheduler address");

        System.out.print("Address : ");
        values[0] = scanner. nextLine();

        while ( !InetAddressUtils.isIPv4Address(values[0])) {
            System.out.println("Address not valid");

            System.out.print("Address : ");
            values[0] = scanner. nextLine();
        }


        return values;
    }


    /**
     * Usage
     *  $> server [--port|-p <number>] <--hostc|-h <address> | <--as-scheduler | --sh> >  [--portc|-pc <number>]  [--ports|-ps <NUMBER>]
     *
     *      --port  | -p  : Puerto en donde el servidor va a recibir mensajes(Opcional, se usara el default)
     *      --hostc | -c  : Address del coordinador (Scheduler), obligatorio para agregarse al grupo si no se pasa --as-scheduler
     *      --portc | -pc : Puerto del coordinador (opcional, se usara el default)
     *      --ports | -ps : Puerto en donde el scheduler recibirá mensajes (Opcional, se usara el default)
     *      --as-scheduler | --sh
     *
     *      Ejemplo 1: Servidor y Scheduler
     *      server --sh

     *      Ejemplo 2: Servidor
     *      server -h 127.0.0.1
     *
     *      Si estan en la misma máquina se pueden utilizar los argumentos de puertos para diferenciarlos.
     *
     * @param args
     */
    public static void main(String[] args) {

        int portServer = Constants.DEFAULT_SERVER_PORT;
        int portScheduler = Constants.DEFAULT_SCHEDULER_PORT;
        int portCoordinator = Constants.DEFAULT_SCHEDULER_PORT;
        boolean isScheduler = false;

        String coordinatorAddress = "";

        if (args.length >= 1) {

            portServer = getArgInt(new String[]{"--port","-p"},args,portServer);

            portScheduler = getArgInt(new String[]{"--ports","-ps"},args,portScheduler);

            coordinatorAddress = getArgStr(new String[]{"--hostc","-c"},args,null);

            portCoordinator = getArgInt(new String[]{"--portc","-pc"},args,portCoordinator);

            isScheduler = existArguments(new String[]{"--as-sh","--sh"},args);
        }

        printOrbWeaverIntro("Server");
        printIPHost();

        if(!isScheduler && StringUtils.isEmpty(coordinatorAddress)){
            String[] addressport = askUserSchedulerAddress(portCoordinator);
            coordinatorAddress = addressport[0];
            portCoordinator = Integer.parseInt(addressport[1]);

        }

        Server scheduler = new Server(portServer,coordinatorAddress,portCoordinator,portScheduler,isScheduler);
        scheduler.run();
    }

    public ArrayList<ServerInfo> getAllServersByServiceName(String serviceName) {

        ArrayList<ServerInfo> servers = new ArrayList<>();

        for(ServerInfo server : this.servers){
            for(ServiceInfo serviceInfo : server.getServices()){
                if (serviceInfo.getName().equals(serviceName)){
                    servers.add(server);
                    break;
                }
            }
        }

        return servers;
    }
}