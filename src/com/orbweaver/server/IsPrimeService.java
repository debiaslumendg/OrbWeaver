package com.orbweaver.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class IsPrimeService implements ServiceInterfaz{

    private String name = "isprime";

    @Override
    public String getName() {
        return name;
    }

    /**
     * Atiende las peticion para ejecutar wordcount, lee los argumentos pasados por el cliente
     * @param dataInputStream
     * @param dataOutputStream
     */
    @Override
    public void handleClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        long nWords = 0;
        String content;
        Gson gson = new Gson();

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Client ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort()), e);
        }

        JsonObject jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();
        long ncheck = jsonObjectMessage.get("n").getAsLong();

        int isprime = isNumberPrime(ncheck);

        content = String.format("{\"is_prime\":%d}",isprime);

        System.out.format("[Server] Sending response '%s' to client (%s,%d) \n" ,
                content,socket.getInetAddress().getHostName(),socket.getPort());

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort()), e);
        }

    }

    /**
     * Regresa el numero de palabras en un string
     * @return
     */
    static int isNumberPrime(long num) {
        int i = 2;
        boolean flag = false;
        while(i <= num/2)
        {
            // condition for nonprime number
            if(num % i == 0)
            {
                flag = true;
                break;
            }
            ++i;
        }
        return (!flag) ? 1:0;
    }

}