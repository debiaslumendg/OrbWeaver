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
 * Esta clase se encarga de enviar el archivo linea por linea a un servidor
 * que se encargará de contar el número de palabras de un archivo.
 *
 * TODO: Se puede generalizar y que el servidor regrese un análisis mayor del archivo, algo que valga la pena tener que
 *      enviar el archivo completo
 *
 */
public class WordCount implements   OnServiceArgumentsToServer{

    private final String fileP;

    public WordCount(String filep) {
        this.fileP = filep;
    }

    /**
     * Una vez lograda la conexión con el servidor, le envía los argumentos al servidor para que realize el servicio
     * @param socket
     * @param dataInputStream
     * @param dataOutputStream
     */
    @Override
    public void onServiceArgumentsToServer(Socket socket,DataInputStream dataInputStream,DataOutputStream dataOutputStream) {

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
                                    socket.getInetAddress().getHostName(),socket.getPort()), e);
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
                            socket.getInetAddress().getHostName(),socket.getPort()), e);
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort()), e);
        }

        ServiceWordCountAnswerMsg serviceWordCountAnswerMsg = gson.fromJson(content, ServiceWordCountAnswerMsg.class);

        System.out.format("File '%s' contains %d word(s).\n",this.fileP,serviceWordCountAnswerMsg.getWordcount());
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
                OnServiceArgumentsToServer wordcount = new WordCount(fpath);
                Client client = new Client("wordcount",portScheduler,schedulerAddress);
                client.setOnServiceArgumentsToServer(wordcount);
                new Thread(client).start();
            }
        }
    }
}
