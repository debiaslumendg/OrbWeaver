package com.orbweaver.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbweaver.commons.*;
import com.orbweaver.server.scheduler.Scheduler;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private int          serverPort     = Constants.DEFAULT_SERVER_PORT;

    private ServerSocket serverSocket   = null;

    private boolean iAmScheduler = false;


    private String coordinatorAddress     = "";
    private int coordinatorPort               = Constants.DEFAULT_SCHEDULER_PORT;;

    private int schedulerPort           = Constants.DEFAULT_SCHEDULER_PORT;

    private ArrayList<ServerInfo> servers = new ArrayList<>();

    /*Next id a asignar a un nuevo servidor agregado al grupo*/
    private int nextServerID = 0;

    /**Lista de requests que está manejando el Scheduler
     * Como todos los servidores son a su vez un backup para el scheduler
     * Esta lista de request debe ser actualizada en todos los servidores
     * */
    private ArrayList<RequestInfo> requests = new ArrayList<>() ;


    private ServerInfo myServerInfo;

    private boolean      isStopped      = false;
    private int myId;

    private OnRequestServiceToClient onRequestServiceToClient;

    public Server(int serverPort,String coordinatorAddress, int coordinatorPort,int schedulerPort,boolean isCheduler){
        this.serverPort = serverPort;
        this.coordinatorAddress = coordinatorAddress;
        this.coordinatorPort = coordinatorPort;
        this.schedulerPort = schedulerPort;

        iAmScheduler = isCheduler;

    }

    public String geteCoordinatorAddress(){
        return this.coordinatorAddress;
    }
    public int getCoordinatorPort(){
        return this.coordinatorPort;
    }

    /**
     * Este metodo se encarga de comunicarse con el Scheduler y decirle que lo agregue al grupo.
     */
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
                            this.coordinatorAddress,this.coordinatorPort), e);
        }


        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        JsonObject jsonObjectMessage;
        String content;

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
        content = gson.toJson(requestAddServerMsg);
        System.out.println("[Server] Sending " + content + " to the Scheduler");

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Scheduler ( %s , %d)",
                            this.coordinatorAddress,this.coordinatorPort)
                    , e);
        }

        try {
            // Obtenemos el contenido del mensaje del scheduler
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot read answer from Scheduler ( %s , %d)",
                            this.coordinatorAddress,this.coordinatorPort)
                    , e);
        }

        // Parseamos el mensaje a JSON
        jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();

        System.out.println(jsonObjectMessage);

        int status = jsonObjectMessage.get("status").getAsInt();

        switch (status){
            case Constants.STATUS_SUCCESS_REQUEST:
                RequestAddServerAnswerMsg requestAddServerAnswerMsg = gson.fromJson(content, RequestAddServerAnswerMsg.class);
                System.out.println("[Server] Got from Scheduler: ");
                servers = requestAddServerAnswerMsg.getServers();
                System.out.println("[Server] \tServers: " + servers);
                requests = requestAddServerAnswerMsg.getRequests();
                System.out.println("[Server] \tRequests: " + requests);


                myId = requestAddServerAnswerMsg.getServerID();
                nextServerID = myId + 1;
                System.out.println("[Server] \tMy ID: " + myId);

                System.out.println("[Server] \tNext server ID: " + nextServerID);

                System.out.println("[Server] Added to the Group...");


                myServerInfo = getServerByID(myId);


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


    public ServerInfo getServerByID(int id){
        for(ServerInfo serverInfo : servers){
            if(serverInfo.getId() == id) {
                return serverInfo;
            }
        }

        return null;
    }



    /**
     *
     * @return lista de servidores miembros del grupo
     */
    public ArrayList<ServerInfo> getServers(){
        return servers;
    }

    public void run(){

        myServerInfo = new ServerInfo(this.serverPort, onRequestServiceToClient.getServicesList());

        servers.add(myServerInfo);

        if(!iAmScheduler) {
            // Si este servidor NO funcionará como Scheduler entonces pedimos que se nos agregue al grupo de servidores
            addServerToGroup();
        }else{
            // Si este servidor funcionará como Scheduler establecemos nuestro estado inicial
            myServerInfo.setId(this.nextServerID);
            myServerInfo.setAddress(Util.getIPHost());
            this.nextServerID++;

            // Iniciamos el scheduler
            new Thread(new Scheduler(this,schedulerPort)).start();

            // TODO: Falta el proceso automático de elección del siguiente Scheduler
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
     * Obtiene todos los servidores que ejecutan un servicio dado
     * @param serviceName Nombre del servicio
     * @return lista de servidores
     */
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

    public ServiceInterfaz getServiceExec(String name) {

        if(onRequestServiceToClient != null){
            return onRequestServiceToClient.getService(name);
        }
        return null;
    }

    public int getId() {
        return this.myId;
    }

    public RequestInfo getRequestByID(String request_id) {
        for(RequestInfo r: requests){
            if(r.getId().equals(request_id)){
                return r;
            }
        }
        return null;
    }

    public void setOnRequestServiceToClient(OnRequestServiceToClient onRequestServiceToClient) {
        this.onRequestServiceToClient = onRequestServiceToClient;
    }

    public ArrayList<RequestInfo> getRequests() {
        return  requests;
    }

    public void removeServerByID(int idServer) {
        for(int i = 0; i < servers.size();i++){
            if(servers.get(i).getId() == idServer){
                servers.remove(i);
                return;
            }
        }
        return;
    }

    public int getNextServerID() {
        return this.nextServerID;
    }

    public void setNextServerID(int n) {
        this.nextServerID = n;
    }
}