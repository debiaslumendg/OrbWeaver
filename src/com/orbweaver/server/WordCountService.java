package com.orbweaver.server;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class WordCountService implements ServiceInterfaz{

    private String name = "wordcount";


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
        int nWords = 0;
        String content;
        Gson gson = new Gson();

        boolean eof = false;
        while(!eof){
            try {
                content = dataInputStream.readUTF();
            } catch (IOException e) {
                System.out.format("[Server] Error: Cannot read JSON from Client ( %s , %d)\n",
                                socket.getInetAddress().getHostName(),socket.getPort());
                return false;
            }

            ServiceWordCountArgMsg serviceWordCountArgMsg;
            serviceWordCountArgMsg = gson.fromJson(content, ServiceWordCountArgMsg.class);

            nWords += countWords(serviceWordCountArgMsg.getLine());
            if(serviceWordCountArgMsg.isEOF()){
                eof = true;
            }

        }

        ServiceWordCountAnswerMsg serviceWordCountAnswerMsg = new ServiceWordCountAnswerMsg(nWords);

        content = gson.toJson(serviceWordCountAnswerMsg);


        System.out.format("[Server] Sending response '%s' to client (%s,%d) \n" ,
                content,socket.getInetAddress().getHostName(),socket.getPort());

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            System.out.format("Error: Cannot write JSON to Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort());
            return false;
        }

        return true;

    }

    private static final int OUT = 0;
    private static final int IN = 1;

    /**
     * Regresa el numero de palabras en un string
     * @param str
     * @return
     */
    static int countWords(String str) {
        int state = OUT;
        int wc = 0;  // word count
        int i = 0;

        // Scan all characters one by one
        while (i < str.length())
        {
            // If next character is a separator, set the
            // state as OUT
            if (str.charAt(i) == ' ' || str.charAt(i) == '\n'
                    || str.charAt(i) == '\t')
                state = OUT;


                // If next character is not a word separator
                // and state is OUT, then set the state as IN
                // and increment word count
            else if (state == OUT)
            {
                state = IN;
                ++wc;
            }

            // Move to next character
            ++i;
        }
        return wc;
    }

}