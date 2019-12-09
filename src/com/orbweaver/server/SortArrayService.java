package com.orbweaver.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class SortArrayService implements ServiceInterfaz{

    private String name = "sortArray";

    @Override
    public String getName() {
        return name;
    }

    /**
     * Atiende las peticion para ejecutar wordcount, lee los argumentos pasados por el cliente
     * @param dataInputStream
     * @param dataOutputStream
     * @return
     */
    @Override
    public boolean handleClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        String content;

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            System.out.format("[Server] Error: Cannot read JSON from Client ( %s , %d)\n",
                    socket.getInetAddress().getHostName(),socket.getPort());
            return false;
        }

        JsonObject jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();
        String[] array = jsonObjectMessage.get("array").getAsString().split("[ ]");
        int[] sortedArray = Arrays.stream(array[0].substring(1, array[0].length()-1).split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();

        Arrays.sort(sortedArray);

        content = String.format("{\"sorted_array\":%s}",Arrays.toString(sortedArray));

        System.out.format("[Server] Sending response '%s' to client (%s,%d) \n" ,
                content,socket.getInetAddress().getHostName(),socket.getPort());

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            System.out.format("Error: Cannot write JSON to Server ( %s , %d)\n",
                            socket.getInetAddress().getHostName(),socket.getPort());
            return false;
        }

        return true;

    }
}