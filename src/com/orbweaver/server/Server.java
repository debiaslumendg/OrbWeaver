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
import java.util.ListIterator;
import java.util.Vector;

public class Server {

    private int          serverPort     = Constants.DEFAULT_SERVER_PORT;

    private ServerSocket serverSocket   = null;

    private boolean iAmScheduler = false;


    private String coordinatorAddress     = "";
    private int coordinatorPort               = Constants.DEFAULT_SCHEDULER_PORT;;

    private int schedulerPort           = Constants.DEFAULT_SCHEDULER_PORT;

    private ArrayList<ServerInfo> servers = new ArrayList<>();

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
            System.out.format("[Scheduler] Error: Cannot connect to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
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

    /**
     * Envia un mensaje al grupo
     * - Nuevo servidor
     * - Request actualizado
     * --- El unico que deberia llamar esta funcion es el scheduler. syncronized deberia hacer los mensajes ordenados
     */
    public synchronized void sendMessageToGroup(String message) {

        Vector<Integer> serversToRemove = new Vector<>();

        ListIterator<ServerInfo> iter = servers.listIterator();
        ServerInfo serverInfo;
        while(iter.hasNext()) {
            serverInfo = iter.next();
            if(serverInfo.getId() != myServerInfo.getId()) {
                if(!sendMessageToServer(message, serverInfo)){
                    serversToRemove.add(serverInfo.getId());
                    iter.remove();
                }
            }
        }

        if(serversToRemove.size() > 0){
            sendMessageToGroup(String.format("{\"code\":%d,\"id_servers\":%s}",
                    Constants.CODE_REQUEST_DEL_SERVER,
                    new Gson().toJson(serversToRemove.toArray())));
        }
    }


    /**
     * Agrega un servidor a su lista de miembros.
     * -    Para agregarse un servidor al grupo se debe comunicar con el coordinador, para nuestro proyecto es el mismo Scheduler.
     * -    El scheduler lo agrega a su lista de miembros interna y le asigna un ID a él y a sus servicios nuevos que el scheduler no conozca.
     * -    El scheduler después de agregarlo a su lista interna le envía un mensaje a todos los miembros del grupo, con
     *      la información del nuevo miembro del grupo para que se actualizen.
     *      Es necesario que todos los miembros sepan quienes son el resto, por si:
     *          * Ocurre un error y el scheduler y el scheduler back up mueren, cualquier puede tomar el lugar de scheduler.
     *              Por lo que necesita la lista de miembros para seleccionar entre ellos el nuevo scheduler.
     *
     * @param requestAddServerMsg Informacion del nuevo servidor a agregar
     * @return ID del nuevo servidor
     */
    public synchronized int addServer(RequestAddServerMsg requestAddServerMsg){

        // Se lee el id del anterior servidor para controlar la eliminación y agregación de servidores
        int newServerID;
        ServerInfo serverInfo = requestAddServerMsg.getServer();

        if(iAmScheduler) {
            // TODO : Cambiar la forma en que se maneja la creacion de ids de los servidores
            newServerID = servers.get(servers.size() - 1).getId() + 1;
            serverInfo.setId(newServerID);
        }else{
            newServerID = serverInfo.getId();
        }

        if(iAmScheduler) {
            sendMessageToGroup(new Gson().toJson(requestAddServerMsg));
        }

        servers.add(serverInfo);

        System.out.format("Added server ID=%d :\n\tAddress - (%s,%d)\n", newServerID, serverInfo.getAddress(), serverInfo.getPort());
        System.out.format("\tServices %s\n", serverInfo.getServices());

        return newServerID;
    }

    /**
     *
     * @return lista de servidores miembros del grupo
     */
    public ArrayList<ServerInfo> getServers(){
        return servers;
    }

    public void run(){

        myServerInfo = new ServerInfo("manuggz","127.0.0.1",this.serverPort, onRequestServiceToClient.getServicesList());

        servers.add(myServerInfo);

        if(!iAmScheduler) {
            // Si este servidor NO funcionará como Scheduler entonces pedimos que se nos agregue al grupo de servidores
            addServerToGroup();
        }else{
            // Si este servidor funcionará como Scheduler establecemos nuestro estado inicial
            myServerInfo.setId(0);

            // Iniciamos el scheduler
            new Thread(new Scheduler(this,schedulerPort)).start();

            // TODO: Falta el proceso automático de elección del siguiente Scheduler
            // Si muere se selecciona otro Scheduler con el algoritmo grandulon
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

    public static void election(ArrayList<ServerInfo> servers, ServerInfo scheduler)
    {
        // Process ed = ProcessElection.getElectionInitiator();

        if((ed.getPid()) == scheduler.getPid()) {
            ServerInfo oldScheduler = scheduler;
            oldScheduler.setDownflag(false);
            scheduler = (ServerInfo) this.servers.get(ed.getPid()-1);
            // ProcessElection.setElectionFlag(false);
            scheduler.setCoordinatorFlag(true);
            System.out.println("\nNew Coordinator is : P" + scheduler.getPid());
        }
        else {
            System.out.print("\n");
            for(int i = ed.getPid()+1; i < this.servers.size(); i++) {
                System.out.println("P" + ed.getPid() + ": Sending message to P" + i);
            }
            ServerInfo a = (ServerInfo) this.servers.get(ed.getPid()+1);

            // ProcessElection.setElectionInitiator(a);
        }
    }
}