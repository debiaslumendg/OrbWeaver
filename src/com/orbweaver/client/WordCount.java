package com.orbweaver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.orbweaver.commons.*;
import com.orbweaver.server.ServiceWordCountAnswerMsg;
import com.orbweaver.server.ServiceWordCountArgMsg;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

import static com.orbweaver.commons.Constants.STATUS_ERROR_REQUEST;
import static com.orbweaver.commons.Util.getArgInt;
import static com.orbweaver.commons.Util.getArgStr;

/**
 * Esta clase es un hilo que corre un cliente cuyo unico objetivo es enviar el archivo linea por linea a un servidor
 * que se encargará de contar el número de palabras de un archivo.
 *
 * TODO: Se puede generalizar y que el servidor regrese un análisis mayor del archivo, algo que valga la pena tener que
 * TODO: enviar el archivo completo
 *
 */
public class WordCount implements   Runnable{

    private final String fileP;
    private int portScheduler;
    private String addressScheduler;
    private String requestId;
    private ServerInfo serverInfo;
    /** Cuando se realiza la peticion de una ejecucion al servidor si esta falla  por error del request_id se prueba a
     *  ejecutar de nuevo la request.
     */
    private boolean mTryAgainRequest;

    /**
     * N intentos en ejecutar la request
     */
    private int ntries = 0;

    public WordCount(String filep, int portScheduler,String addressScheduler) {

        this.portScheduler = portScheduler;
        this.addressScheduler = addressScheduler;
        this.fileP = filep;
    }

    /**
     * Se comunica con el Scheduler para pedirle la ejecución de un servicio
     * @return clase request respuesta del scheduler
     */
    private boolean getRequestScheduler(){

        RequestServiceAnswerMsg requestServiceAnswerMsg = null;
        System.out.format("Connecting to Scheduler(%s,%d)\n",this.addressScheduler,this.portScheduler);

        Socket socketScheduler = null;

        try {
            socketScheduler = new Socket(this.addressScheduler,this.portScheduler);
            System.out.println("Connected to Scheduler");
        } catch (IOException e) {
            // TODO: Puede time out
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }


        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        JsonObject jsonObjectMessage;
        String json;

        try {
            dataOutputStream    = new DataOutputStream(socketScheduler.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socketScheduler.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Cannot open connection to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }

        RequestServiceMsg requestServiceMsg = new RequestServiceMsg("wordcount");
        json = gson.toJson(requestServiceMsg);

        System.out.println("Sending " + json + " to the Scheduler");

        try {
            dataOutputStream.writeUTF(json);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }

        try {
            // Obtenemos el contenido del mensaje del scheduler
            json = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot read answer from Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }


        requestServiceAnswerMsg = gson.fromJson(json, RequestServiceAnswerMsg.class);

        switch (requestServiceAnswerMsg.getStatus()){
            case Constants.STATUS_SUCCESS_REQUEST:
                System.out.println("[Server] Got from Scheduler: ");
                requestId = requestServiceAnswerMsg.getRequestId();
                System.out.println("[Server] \trequestId: " + requestId);
                serverInfo = requestServiceAnswerMsg.getServerInfo();
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

        return requestServiceAnswerMsg != null;
    }

    /**
     * Se comunica con el Servidor para enviarle la request de servicio
     *
     * TODO: Falta manejo de errores así como en todo el programa por eso no unifico todavía el código parecido para los
     *      exception ya que cada exception se puede manejar distinto dependiendo de la clase y mensaje
     */
    private void sendRequestToServer(){

        Socket socket;

        try {
            socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }

        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;

        try {
            dataOutputStream    = new DataOutputStream(socket.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Client] Error: Cannot open connection to Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }


        RequestServiceMsg requestServiceMsg = new RequestServiceMsg("wordcount");
        requestServiceMsg.setIdRequest(requestId);
        content = gson.toJson(requestServiceMsg);

        System.out.format("[Client] Sending " + content + " to the Server (%s,%s, %d)\n",
                serverInfo.getName(),serverInfo.getAddress(),serverInfo.getPort());

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Client] Error: Cannot read JSON from Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }


        System.out.println("[Client] Received from Server" + content);

        RequestAnswerMsg requestAnswerMsg = gson.fromJson(content, RequestAnswerMsg.class);

        switch (requestAnswerMsg.getStatus()){
            case Constants.STATUS_SUCCESS_REQUEST:
                sendArgsWordCountService(socket,dataInputStream,dataOutputStream);
                break;
            case STATUS_ERROR_REQUEST:
                Util.mostrarErrorPrint(requestAnswerMsg.getCode());
                mTryAgainRequest = true;
                break;
        }

        try {
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Una vez lograda la conexión con el servidor, le envía los argumentos al servidor para que realize el servicio
     * @param socket
     * @param dataInputStream
     * @param dataOutputStream
     */
    private void sendArgsWordCountService(Socket socket,DataInputStream dataInputStream,DataOutputStream dataOutputStream) {

        Gson gson = new Gson();
        String content;
        ServiceWordCountArgMsg serviceWordCountArgMsg;

        try (Stream<String> lines = Files.lines(Paths.get(this.fileP), StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator){
                serviceWordCountArgMsg = new ServiceWordCountArgMsg(0,line);
                content = gson.toJson(serviceWordCountArgMsg);

                try {
                    dataOutputStream.writeUTF(content);
                } catch (IOException e) {
                    throw new RuntimeException(
                            String.format("Error: Cannot write JSON to Server ( %s , %d)",
                                    socket.getInetAddress().getHostName(),socket.getPort())
                            , e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot read file '%s' line ",
                            this.fileP)
                    , e);
        }

        serviceWordCountArgMsg = new ServiceWordCountArgMsg(1,"");
        content = gson.toJson(serviceWordCountArgMsg);

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort())
                    , e);
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }

        ServiceWordCountAnswerMsg serviceWordCountAnswerMsg = gson.fromJson(content, ServiceWordCountAnswerMsg.class);

        System.out.format("File '%s' contains %d word(s).\n",this.fileP,serviceWordCountAnswerMsg.getWordcount());
    }

    @Override
    public void run() {

        mTryAgainRequest = true;
        ntries = 0;
        while(mTryAgainRequest && ntries <3) {
            mTryAgainRequest = false;
            if (getRequestScheduler() && StringUtils.isNotEmpty(requestId)) {
                sendRequestToServer();
            }
            ntries ++;
        }

    }

    /**
     * Usage
     *  $> wordcount <LIST OF FILES>  [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]
     *
     *      LIST OF FILES   : Obligatorio, lista de rutas a archivos.
     *      --hosts  | -hs  : Address del coordinador (Scheduler) , opcional se usará el de un archivo de configuración
     *      --ports  | -ps  : Puerto del coordinador (Scheduler), opcional se usará el de un archivo de configuración
     *
     * @param args
     */
    public static void main(String[] args) {

        int portScheduler = Constants.DEFAULT_SCHEDULER_PORT;
        String schedulerAddress = "";

        boolean anyError = false;
        ArrayList<String> filesPath = new ArrayList<>();

        if (args.length >= 1) {

            portScheduler = getArgInt(new String[]{"--ports", "-ps"}, args, portScheduler);

            schedulerAddress = getArgStr(new String[]{"--hosts", "-hs"}, args, "");


            for(int i = 0; i < args.length ; i++) {
                if(args[i].startsWith("-")){
                    i++;
                }else {
                    filesPath.add(args[i]);
                }
            }
        }else{
            System.out.println("Usage:wordcount  <List of files> [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]");
            System.out.println();
            System.exit(-1);
        }


        File tmp;
        for(String fpath : filesPath){
            tmp = new File(fpath);
            if(!tmp.exists()){
                anyError = true;
                System.out.println("Error file: '" + fpath + "' not found.");
            }else if(!tmp.isFile()){
                anyError = true;
                System.out.println("Error '" + fpath + "' is not a file.");
            }
        }

        if(anyError){
            System.out.println("Error: aborting execution.");
            System.exit(-1);
        }else{
            for(String fpath : filesPath){
                new Thread(new WordCount(fpath,portScheduler,schedulerAddress)).start();
            }

        }


    }
}
