package com.orbweaver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbweaver.commons.*;

import java.io.*;
import java.net.Socket;

import static com.orbweaver.commons.Util.getArgInt;
import static com.orbweaver.commons.Util.getArgStr;

/**
 * Envia una solicitud de un servicio al scheduler
 *
 * Solicitud: Es x un numero primo?
 */
public class EsPrimo implements OnServiceArgumentsToServer {

    private long ncheck;

    public EsPrimo(long ncheck) {
        this.ncheck = ncheck;
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

        content = String.format("{\"n\":%d}",ncheck);

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
                    String.format("Error: Cannot read JSON from Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort()), e);
        }

        // Parseamos el mensaje a JSON
        JsonObject jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();

        System.out.println("[Server] " + jsonObjectMessage);

        System.out.format("Number '%d' %ses primo.\n",this.ncheck,(jsonObjectMessage.get("is_prime").getAsInt() == 1)?"":"no ");
    }


    /**
     * Usage
     *  $> esprimo <NUMBER>  [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]
     *
     *      NUMBER   : Obligatorio, numero a verificar si es primo
     *      --hosts  | -hs  : Address del coordinador (Scheduler) , opcional se usará el de un archivo de configuración
     *      --ports  | -ps  : Puerto del coordinador (Scheduler), opcional se usará el de un archivo de configuración
     *
     * @param args
     */
    public static void main(String[] args) {

        int portScheduler = Constants.DEFAULT_SCHEDULER_PORT;
        String schedulerAddress = "";

        boolean anyError = false;
        long ntocheck = 0;

        if (args.length >= 1) {

            portScheduler = getArgInt(new String[]{"--ports", "-ps"}, args, portScheduler);

            schedulerAddress = getArgStr(new String[]{"--hosts", "-hs"}, args, "");

            anyError = true;
            for(int i = 0; i < args.length ; i++) {
                if(args[i].startsWith("-")){
                    i++;
                }else {
                    ntocheck = Long.parseLong(args[i]);
                    anyError = false;
                }
            }
        }else{
            System.out.println("Usage:esprimo  <NUMBER> [--hosts|-hs <NUMBER>] [--ports|-ps <NUMBER>]");
            System.out.println();
            System.exit(-1);
        }

        if(anyError){
            System.out.println("Error: aborting execution.");
            System.exit(-1);
        }else{
            OnServiceArgumentsToServer esPrimo = new EsPrimo(ntocheck);

            Client client = new Client("isprime",portScheduler,schedulerAddress);
            client.setOnServiceArgumentsToServer(esPrimo);
            client.run();

        }
    }
}
