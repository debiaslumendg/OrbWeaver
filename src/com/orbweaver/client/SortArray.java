package com.orbweaver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbweaver.commons.*;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

import static com.orbweaver.commons.Util.getArgInt;
import static com.orbweaver.commons.Util.getArgStr;

/**
 * Envia una solicitud de un servicio al scheduler
 *
 * Solicitud: Ordenar un arreglo
 */
public class SortArray implements OnServiceArgumentsToServer {

    private int[] array;

    public SortArray(int[] array) {
        this.array = array;
    }

    /**
     * Una vez lograda la conexión con el servidor, le envía los argumentos para que realize el servicio
     * @param socket
     * @param dataInputStream
     * @param dataOutputStream
     */
    @Override
    public void onServiceArgumentsToServer(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {

        Gson gson = new Gson();
        String content;

        content = String.format("{\"array\":\"%s\"}",Arrays.toString(array));

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Client] Error: Cannot write JSON to Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort()), e);
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort()), e);
        }

        // Parseamos el mensaje a JSON
        JsonObject jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();

        System.out.format("Sorted array %s.\n",(jsonObjectMessage.get("sorted_array").getAsString()));
    }


    /**
     * Usage
     *  $> sortArray <ARRAY>  [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]
     *
     *      ARRAY   : Obligatorio, arreglo a ordenar
     *      --hosts  | -hs  : Address del coordinador (Scheduler) , opcional se usará el de un archivo de configuración
     *      --ports  | -ps  : Puerto del coordinador (Scheduler), opcional se usará el de un archivo de configuración
     *
     * @param args
     */
    public static void main(String[] args) {

        int portScheduler = Constants.DEFAULT_SCHEDULER_PORT;
        String schedulerAddress = "";

        boolean anyError = false;
        int[] arrayToSort = {};
        
        if (args.length >= 1) {
            
            portScheduler = getArgInt(new String[]{"--ports", "-ps"}, args, portScheduler);
            
            schedulerAddress = getArgStr(new String[]{"--hosts", "-hs"}, args, "");

            anyError = true;
            for(int i = 0; i < args.length ; i++) {
                if(args[i].startsWith("-")){
                    i++;
                }else {
                    String[] arr = args[i].split("[ ]");
                    arrayToSort = Arrays.stream(arr[0].substring(1, arr[0].length()-1).split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
                    anyError = false;
                }
            }
        }else{
            System.out.println("Usage:sortArray  <ARRAY> [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]");
            System.out.println();
            System.exit(-1);
        }

        if(anyError){
            System.out.println("Error: aborting execution.");
            System.exit(-1);
        }else{
            OnServiceArgumentsToServer sortArray = new SortArray(arrayToSort);

            Client client = new Client("sortArray",portScheduler,schedulerAddress);
            client.setOnServiceArgumentsToServer(sortArray);
            client.run();

        }
    }
}
