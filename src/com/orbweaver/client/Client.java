package com.orbweaver.client;

import com.google.gson.*;
import com.orbweaver.commons.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.stream.Stream;

import static com.orbweaver.commons.Constants.*;

public class Client implements Runnable {

    /*Ruta al archivo con la lista de miembros*/
    public static final String CLIENT_PATH_TO_MEMBERS_TXT = "members.txt";
    public static final String CLIENT_PATH_TO_SCHEDULER_TXT = "scheduler.txt";

    private int portScheduler;
    private String addressScheduler;

    private String serviceName;
    /** Cuando se realiza la peticion de una ejecucion al servidor si esta falla  por error del request_id se prueba a
     *  ejecutar de nuevo la request.
     */
    //private boolean mTryAgainRequest;

    private OnServiceArgumentsToServer onServiceArgumentsToServer;
    private ArrayList<ServerInfo> members;

    public Client(String serviceName,int portScheduler, String schedulerAddress) {
        this.serviceName = serviceName;
        this.portScheduler = portScheduler;
        this.addressScheduler = schedulerAddress;
    }

    public RequestServiceAnswerMsg getRequestScheduler() {
        return getRequestScheduler("");
    }

    /**
     * Se comunica con el Scheduler para pedirle la ejecución de un servicio
     * @return clase request respuesta del scheduler
     */
    public RequestServiceAnswerMsg getRequestScheduler(String oldRequestId){

        RequestServiceAnswerMsg requestServiceAnswerMsg = null;
        System.out.format("Connecting to Scheduler(%s,%d)\n",this.addressScheduler,this.portScheduler);

        Socket socketScheduler = null;

        try {
            socketScheduler = new Socket(this.addressScheduler,this.portScheduler);
            System.out.format("Connected to Scheduler ( %s , %d)\n",
                    this.addressScheduler,this.portScheduler);
        } catch (IOException e) {
            System.out.format("Error: Cannot connect to Scheduler ( %s , %d)\n",
                            this.addressScheduler,this.portScheduler);
            return null;
        }

        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String json;

        try {
            dataOutputStream    = new DataOutputStream(socketScheduler.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socketScheduler.getInputStream()));
        } catch (IOException e) {
            System.out.format("Cannot open connection to Scheduler ( %s , %d)\n",
                            this.addressScheduler,this.portScheduler);
            return null;
        }

        RequestServiceMsg requestServiceMsg = new RequestServiceMsg(serviceName);
        if(StringUtils.isNotEmpty(oldRequestId)){
            requestServiceMsg.setIdRequest(oldRequestId);
        }

        json = gson.toJson(requestServiceMsg);

        //System.out.println("Sending " + json + " to the Scheduler");

        try {
            dataOutputStream.writeUTF(json);
        } catch (IOException e) {
            System.out.format("Error: Cannot write JSON to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler);
            return null;
        }

        try {
            // Obtenemos el contenido del mensaje del scheduler
            json = dataInputStream.readUTF();
        } catch (IOException e) {
            System.out.format("Error: Cannot read answer from Scheduler ( %s , %d)\n",
                            this.addressScheduler,this.portScheduler);
            return null;
        }


        requestServiceAnswerMsg = gson.fromJson(json, RequestServiceAnswerMsg.class);

        switch (requestServiceAnswerMsg.getStatus()){
            case Constants.STATUS_SUCCESS_REQUEST:
                System.out.println("[Server] Got from Scheduler: ");
                String requestId = requestServiceAnswerMsg.getRequestId();
                System.out.println("[Server] \trequestId: " + requestId);
                ServerInfo serverInfo = requestServiceAnswerMsg.getServerInfo();
                System.out.println("[Server] \tserverInfo: " + serverInfo);

                break;
            case STATUS_ERROR_REQUEST:
                Util.mostrarErrorPrint(requestServiceAnswerMsg.getCode());
                break;
        }

        try {
            dataOutputStream.close();
            dataInputStream.close();
            socketScheduler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return requestServiceAnswerMsg;
    }
    /**
     * Se comunica con el Servidor para enviarle la request de servicio
     *
     * TODO: Falta manejo de errores así como en todo el programa por eso no unifico todavía el código parecido para los
     *      exception ya que cada exception se puede manejar distinto dependiendo de la clase y mensaje
     * @return
     */
    private boolean sendRequestToServer(ServerInfo serverInfo, String requestId){

        Socket socket;

        try {
            socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        } catch (IOException e) {
            System.out.format("Error: Cannot connect to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }

        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;

        try {
            dataOutputStream    = new DataOutputStream(socket.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            System.out.format("[Client] Error: Cannot open connection to Server ( %s , %d)\n",
                    serverInfo.getAddress(), serverInfo.getPort());
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            return false;
        }


        RequestServiceMsg requestServiceMsg = new RequestServiceMsg(serviceName);
        requestServiceMsg.setIdRequest(requestId);
        content = gson.toJson(requestServiceMsg);

        System.out.format("[Client] Sending " + content + " to the Server (%s, %d)\n",
                serverInfo.getAddress(),serverInfo.getPort());

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            System.out.format("[Client] Error: Cannot write JSON to Server ( %s , %d)\n",
                    serverInfo.getAddress(), serverInfo.getPort());
            try {
                dataOutputStream.close();
                dataInputStream.close();
                socket.close();
            } catch (IOException ignored) {
            }
            return false;
        }

        // Aquí el servidor actualizó el estado de la request en el scheduler
        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            System.out.format("[Client] Error: Cannot read JSON from Server ( %s , %d)\n",
                    serverInfo.getAddress(), serverInfo.getPort());
            try {
                dataOutputStream.close();
                dataInputStream.close();
                socket.close();
            } catch (IOException ignored) {
            }
            return false;
        }

        System.out.println("[Client] Received from Server" + content);

        RequestAnswerMsg requestAnswerMsg = gson.fromJson(content, RequestAnswerMsg.class);

        if (requestAnswerMsg.getStatus() == STATUS_SUCCESS_REQUEST) {
            if (onServiceArgumentsToServer != null) {
                onServiceArgumentsToServer.onServiceArgumentsToServer(socket, dataInputStream, dataOutputStream);
            }
        }

        try {
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(requestAnswerMsg.getStatus() == STATUS_ERROR_REQUEST){
            Util.mostrarErrorPrint(requestAnswerMsg.getCode());
            return false;
        }
        return true;
    }

    @Override
    public void run() {

        RequestServiceAnswerMsg answerScheduler;
        /**
         * N intentos en ejecutar la request
         */
        int ntries = 0;

        if(StringUtils.isEmpty(addressScheduler)){
            if(!readAddressSchedulerFromFile()){
                if(!askWhoIsSchedulerToMembers()){
                    if(StringUtils.isEmpty(addressScheduler)){
                        System.out.println("There's no scheduler configured yet.");
                        System.out.println("Please, add arguments [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>].");
                        saveFiles();
                        return;
                    }
                }
            }
        }else{
            updateMembersList();
        }

        answerScheduler = tryUntilGetRequestScheduler("");

        if(answerScheduler == null ||answerScheduler.isError()){
            saveFiles();
            return;
        }

        while(ntries <3) {

            if(sendRequestToServer(answerScheduler.getServerInfo(),answerScheduler.getRequestId())) break; ;

            answerScheduler = tryUntilGetRequestScheduler(answerScheduler.getRequestId());
            if(answerScheduler == null||answerScheduler.isError()){
                break;
            }
            ntries++;
        }

        if(ntries >= 3){
            System.out.println("Error ejecutando el servicio!");
        }
        saveFiles();
    }

    /**
     * Guarda los archivos de configuración , con los datos actuales
     */
    private void saveFiles() {
        FileWriter fileWScheduler  = null;
        PrintWriter printWScheduler = null;
        try {
            fileWScheduler = new FileWriter(CLIENT_PATH_TO_SCHEDULER_TXT);
            printWScheduler = new PrintWriter(fileWScheduler);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        printWScheduler.printf("%s %d", this.addressScheduler, this.portScheduler);
        printWScheduler.close();

        if(members == null) return;
        FileWriter fileWMembers = null;
        PrintWriter printWMembers = null;
        try {
            fileWMembers = new FileWriter(CLIENT_PATH_TO_MEMBERS_TXT);
            printWMembers = new PrintWriter(fileWMembers);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for(ServerInfo serverInfo : members){
            printWMembers.printf("%s %d",serverInfo.getAddress(),serverInfo.getPort());
        }
        printWMembers.close();

    }

    private void updateMembersList() {

        System.out.format("Connecting to Scheduler(%s,%d)\n",this.addressScheduler,this.portScheduler);

        Socket socketScheduler = null;

        try {
            socketScheduler = new Socket(this.addressScheduler,this.portScheduler);
            System.out.format("Connected to Scheduler ( %s , %d)\n",this.addressScheduler,this.portScheduler);
        } catch (IOException e) {
            System.out.format("Error: Cannot connect to Scheduler ( %s , %d)\n",this.addressScheduler,this.portScheduler);
            System.exit(1);
        }

        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream = null;

        String content;

        try {
            dataOutputStream    = new DataOutputStream(socketScheduler.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socketScheduler.getInputStream()));
        } catch (IOException e) {
            System.out.format("Cannot open connection to Scheduler ( %s , %d)\n",
                    this.addressScheduler,this.portScheduler);
            System.exit(1);
            return;
        }



        try {
            dataOutputStream.writeUTF(String.format("{code:%d}",CODE_MESSAGE_GET_MEMBERS_LIST));
        } catch (IOException e) {
            System.out.format("Error: Cannot write JSON to Scheduler ( %s , %d)",
                    this.addressScheduler,this.portScheduler);
            System.exit(1);
        }

        try {
            // Obtenemos el contenido del mensaje del scheduler
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            System.out.format("Error: Cannot read answer from Scheduler ( %s , %d)\n",
                    this.addressScheduler,this.portScheduler);
            System.exit(1);
            return ;
        }

        JsonArray jsonArrayMembers;

        // Parseamos el mensaje a JSON
        try {
            jsonArrayMembers = new JsonParser().parse(content).getAsJsonArray();
        }catch (JsonSyntaxException e){
            System.out.println("Error : incorrect message sent by scheduler");
            System.out.println("Message: " + content);
            System.exit(1);
            return ;
        }

        this.members = new ArrayList<ServerInfo>();
        for(JsonElement serverInfoJson : jsonArrayMembers) {
            this.members.add(gson.fromJson(serverInfoJson,ServerInfo.class));
        }
        try {
            dataOutputStream.close();
            dataInputStream.close();
            socketScheduler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private RequestServiceAnswerMsg tryUntilGetRequestScheduler(String oldRequesr) {
        RequestServiceAnswerMsg answerScheduler = getRequestScheduler(oldRequesr);
        while (answerScheduler == null){
            if(!askWhoIsSchedulerToMembers()){
                System.out.println("Couldn't ask who is Scheduler to members.");
                System.out.println("Please, add argument(s) [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>].");
                return null;
            }else{
                answerScheduler = getRequestScheduler();
            }
        }

        return answerScheduler;
    }

    /**
     * Lee la lista de miembros desde el archivo
     * @return
     */
    private ArrayList<ServerInfo> readMembersFromFile(){

        ArrayList<ServerInfo> members = new ArrayList<>();

        String[] contentLine;
        try (Stream<String> lines = Files.lines(Paths.get(CLIENT_PATH_TO_MEMBERS_TXT), StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator){
                contentLine = line.split("[\t| ]+");
                ServerInfo serverInfo = new ServerInfo();
                serverInfo.setAddress(contentLine[0]);
                this.addressScheduler = contentLine[0];
                serverInfo.setPort(Constants.DEFAULT_SERVER_PORT);
                if(contentLine.length > 1){
                    if(!contentLine[1].equals("-")) {
                        serverInfo.setPort(Integer.parseInt(contentLine[1]));
                    }
                }
                members.add(serverInfo);
            }
        } catch (IOException e) {
            System.out.format("Error: Cannot read file '%s' line\n", CLIENT_PATH_TO_MEMBERS_TXT);
        }

        return members;

    }

    /**
     * Envía un mensaje al miembro especificado preguntando quién es el Scheduler
     * @param serverInfo
     * @return
     */
    public boolean askWhoIsSchedulerToMember(ServerInfo serverInfo){

        Socket socket;

        try {
            socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        } catch (IOException e) {
            System.out.format("[Scheduler] Error: Cannot connect to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }


        DataOutputStream dataOutputStream;
        DataInputStream dataInputStream;
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.format("Error: Cannot open connection to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }

        try {
            dataOutputStream.writeUTF(String.format("{code:%d}",CODE_MESSAGE_WHO_IS_SCHEDULER));
        } catch (IOException e) {
            System.out.format("Error: Coudn't  write to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }

        String content;

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            System.out.format("Error: Coudn't  read from Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }

        ServerInfo scheduler = new Gson().fromJson(content, ServerInfo.class);

        this.addressScheduler = scheduler.getAddress();
        this.portScheduler = scheduler.getPort();

        try {
            dataInputStream.close();
            dataOutputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;

    }

    /**
     * Este metodo le pregunta la primer miembro que no falle quien es el Scheduler y espera su respuesta.
     * Si todos fallan regresa false, sino true, se presume el miembro preguntado siempre responde.
     */
    private boolean askWhoIsSchedulerToMembers() {

        if(members == null || members.size() == 0){
            members = readMembersFromFile();
        }

        ListIterator<ServerInfo> iter = this.members.listIterator();
        ServerInfo serverInfo;
        while(iter.hasNext()) {
            serverInfo = iter.next();

            // Envia el mensaje a todos menos a él mismo
            if(!askWhoIsSchedulerToMember(serverInfo)){
                iter.remove();
            }else{
                return true;
            }
        }
        return false;
    }

    /**
     * Lee la dirección del scheduler del archivo de configuración.
     */
    private boolean readAddressSchedulerFromFile() {

        String[] contentLine;
        try (Stream<String> lines = Files.lines(Paths.get(CLIENT_PATH_TO_SCHEDULER_TXT), StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator){
                contentLine = line.split("[\t| ]+");
                this.addressScheduler = contentLine[0];
                if(contentLine.length > 1){
                    if(!contentLine[1].equals("-")) {
                        // Para evitar modificar el puerto que el usuario paso por la linea de comandos
                        if(this.portScheduler == DEFAULT_SCHEDULER_PORT) {
                            this.portScheduler = Integer.parseInt(contentLine[1]);
                        }
                    }
                }
                return true;
            }
        } catch (IOException e) {
            System.out.format("Error: Cannot read file '%s' line\n", CLIENT_PATH_TO_SCHEDULER_TXT);
        }
        return false;
    }

    public void setOnServiceArgumentsToServer(OnServiceArgumentsToServer onServiceArgumentsToServer) {
        this.onServiceArgumentsToServer = onServiceArgumentsToServer;
    }

    public OnServiceArgumentsToServer getOnServiceArgumentsToServer() {
        return onServiceArgumentsToServer;
    }
}
