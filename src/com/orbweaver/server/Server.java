package com.orbweaver.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbweaver.commons.*;
import com.orbweaver.server.scheduler.Scheduler;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static com.orbweaver.commons.Constants.CODE_MESSAGE_COORDINATOR;
import static com.orbweaver.commons.Constants.CODE_MESSAGE_ELECCION;

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

    // Indica si se ha iniciado una eleccion para grandulon
    private boolean eleccionStarted = false;
    // Indica el tiempo en el que se ha iniciado la eleccion
    private long timeMsEleccionStarted;

    // Cuando el scheduler vuelva a estar disponible se tienen que establecer como hecho las request que tienen estos ids
    private LinkedList<String> listRequestIdToCompleteWhenSchedulerAvailable = new LinkedList<String>() ;

    public Server(int serverPort,String coordinatorAddress, int coordinatorPort,int schedulerPort,boolean isCheduler){
        this.serverPort = serverPort;
        this.coordinatorAddress = coordinatorAddress;
        this.coordinatorPort = coordinatorPort;
        this.schedulerPort = schedulerPort;

        iAmScheduler = isCheduler;

    }

    public String getCoordinatorAddress(){
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
     * Envía un mensaje al servidor especificado
     * Este metodo debería ejecutarlo solo el Scheduler
     * @param message
     * @param serverInfo
     * @return
     */
    public boolean sendMessageToServer(String message, ServerInfo serverInfo){

        Socket socket;

        try {
            socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        } catch (IOException e) {
            System.out.format("[Server] Error: Cannot connect to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }


        DataOutputStream dataOutputStream;
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            System.out.format("Error: Cannot open connection to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }

        try {
            dataOutputStream.writeUTF(message);
            socket.close();
        } catch (IOException e) {
            System.out.format("Error: Coudn't  write to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }

        return true;

    }

    public boolean pingServer(ServerInfo server) {

        if (sendMessageToServer(String.format(
                "{\"code\":%d}", Constants.CODE_MESSAGE_PING
        ), server)) {
            return true;
        } else {
            return false;
        }
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
            setAsScheduler();
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

    public void setAsScheduler() {

        this.iAmScheduler = true;

        // Iniciamos el scheduler
        new Thread(new Scheduler(this,schedulerPort)).start();
    }

    public void setEleccionStarted(boolean started) {
        this.eleccionStarted = started;
    }

    public boolean isEleccionStarted() {
        return eleccionStarted;
    }

    public void setTimeMsEleccionStarted(long currentTimeMillis) {
        this.timeMsEleccionStarted = currentTimeMillis;
    }

    public long getTimeMsEleccionStarted() {
        return timeMsEleccionStarted;
    }

    public boolean isScheduler() {
        return iAmScheduler;
    }

    public void setScheduler(ServerInfo serverInfo) {
        this.coordinatorAddress = serverInfo.getAddress();
        this.schedulerPort = serverInfo.getPort();
    }

    public synchronized void addRequestIdToCompleteWhenSchedulerAvailable(String requestServiceMsg) {
        listRequestIdToCompleteWhenSchedulerAvailable.add(requestServiceMsg);
    }
    /**
     * Se comunica con el scheduler para actualizar el estado de una request
     * Scheduler responde Error:
     *  - Si la request es inválida
     *  - Si la request ya está resuelta
     *  Sino:
     *  - Si la request es válida , establece el servidor como ejecutandola
     */
    public RequestAnswerMsg updateStatusRequestScheduler(String idRequest, RequestInfo.StatusRequest newStatus) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Socket socket;

        try {
            socket = new Socket(getCoordinatorAddress(),getCoordinatorPort());
        } catch (IOException e) {
            System.out.format("Error: Cannot connect to Scheduler ( %s , %d)\n",
                    getCoordinatorAddress(), getCoordinatorPort());
            return null;
        }

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;


        // Obtenemos el contenido del mensaje del cliente
        try{
            dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.format("Error: Cannot connect to Scheduler ( %s , %d)\n",
                    getCoordinatorAddress(), getCoordinatorPort());
            return null;
        }

        RequestUpdateRequestMsg requestUpdateRequestMsg = new RequestUpdateRequestMsg(
                getId(),
                idRequest,
                newStatus
        );
        content = gson.toJson(requestUpdateRequestMsg);
        System.out.println("[Server] Updating request to Scheduler : " + content);

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            System.out.format("Error: Cannot write JSON to Server ( %s , %d)\n",
                    socket.getInetAddress().getHostName(),socket.getPort());
            return null;
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            System.out.format("[Server] Error: Cannot read JSON from Scheduler ( %s , %d)\n",
                    getCoordinatorAddress(), getCoordinatorPort());
            return null;
        }

        System.out.println("[Server] Updating request to Scheduler answer : " + content);
        RequestAnswerMsg answer = gson.fromJson(content, RequestAnswerMsg.class);

        try{
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }

        return answer;

    }

    public void triggerSavedRequestUpdates() {
        RequestAnswerMsg updateRequest;
        while(!listRequestIdToCompleteWhenSchedulerAvailable.isEmpty()){
            String requestID = listRequestIdToCompleteWhenSchedulerAvailable.remove();
            updateRequest = updateStatusRequestScheduler(requestID, RequestInfo.StatusRequest.DONE);
            if(updateRequest == null) {
                startEleccion();
                addRequestIdToCompleteWhenSchedulerAvailable(requestID);
            }else if(updateRequest.isSuccess()) {
                RequestInfo myrequest = getRequestByID(requestID);
                myrequest.setStatus(RequestInfo.StatusRequest.DONE);
            }
        }
    }
    /**
     * Le envía un mensaje a todos los miembros del grupo con ID mayor que él que inicien la eleccion
     */
    public void startEleccion() {

        if(isEleccionStarted() && System.currentTimeMillis() < getTimeMsEleccionStarted())return;

        setEleccionStarted(true);
        setTimeMsEleccionStarted(System.currentTimeMillis());

        boolean anyResponse = false;
        for (ServerInfo serverInfo:getServers()) {
            if(serverInfo.getId() > getId()) {
                if(sendMessageToServer(String.format("{code:%d}", CODE_MESSAGE_ELECCION), serverInfo)){
                    anyResponse = true;
                }
            }
        }
        if(!anyResponse){
            String myServerInfo = new Gson().toJson(getServerByID(getId()));
            setAsScheduler();
            for (ServerInfo serverInfo:getServers()) {
                if(serverInfo.getId() < getId()) {
                    sendMessageToServer(String.format("{code:%d,scheduler:%s}",CODE_MESSAGE_COORDINATOR,
                            myServerInfo),serverInfo);
                    triggerSavedRequestUpdates();
                }
            }
            setEleccionStarted(false);
        }
    }


    public void addRequest(RequestInfo requestInfo) {
        this.requests.add(requestInfo);
    }
}