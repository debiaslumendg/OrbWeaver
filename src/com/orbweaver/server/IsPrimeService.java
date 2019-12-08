package com.orbweaver.server;

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
        long ncheck = jsonObjectMessage.get("n").getAsLong();

        int isprime = isNumberPrime(ncheck);

        content = String.format("{\"is_prime\":%d}",isprime);

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