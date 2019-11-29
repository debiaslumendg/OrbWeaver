package com.orbweaver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.orbweaver.commons.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

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

    public WordCount(String filep, int portScheduler,String addressScheduler) {

        this.portScheduler = portScheduler;
        this.addressScheduler = addressScheduler;
        this.fileP = filep;
    }

    @Override
    public void run() {

        try (Stream<String> lines = Files.lines(Paths.get(this.fileP), StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator){
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot read file '%s' line ",
                            this.fileP)
                    , e);
        }

        System.out.format("[Server] Connecting to Scheduler(%s,%d)\n",
                this.addressScheduler,this.portScheduler);

        Socket socketScheduler = null;
        try {
            socketScheduler = new Socket(this.addressScheduler,this.portScheduler);
            System.out.println("[Server] Connected to Scheduler");
        } catch (IOException e) {
            // TODO: Puede time out
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot connect to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
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
                            this.addressScheduler,this.portScheduler)
                    , e);
        }

        RequestServiceMsg requestServiceMsg = new RequestServiceMsg("wordcount");
        json = gson.toJson(requestServiceMsg);
        System.out.println("[Server] Sending " + json + " to the Scheduler");

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

        // Parseamos el mensaje a JSON
        try {
            jsonObjectMessage = new JsonParser().parse(json).getAsJsonObject();
        }catch (JsonSyntaxException e){
            System.out.println("Error : incorrect message sent by Scheduler");
            System.out.println("Message: " + json);
            throw new RuntimeException(
                    String.format("Error: incorrect message sent by Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);

        }

        System.out.println(jsonObjectMessage);

        int status = jsonObjectMessage.get("status").getAsInt();

        switch (status){
            case Constants.STATUS_SUCCESS_REQUEST:
                RequestServiceAnswerSuccessMsg requestServiceAnswerSuccessMsg = gson.fromJson(json, RequestServiceAnswerSuccessMsg.class);
                System.out.println("[Server] Got from Scheduler: ");
                requestId = requestServiceAnswerSuccessMsg.getRequestId();
                System.out.println("[Server] \trequestId: " + requestId);
                serverInfo = requestServiceAnswerSuccessMsg.getServerInfo();
                System.out.println("[Server] \tserverInfo: " + serverInfo);


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
