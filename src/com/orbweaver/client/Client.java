package com.orbweaver.client;

import com.orbweaver.commons.RequestServiceMsg;
import com.google.gson.Gson;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    public static void client(String servicio, String parametro) throws Exception {
        
         // IP donde se hará la conexión
         InetAddress direccion;
         // Canal de comunicación con el servidor
         Socket servidor;
 
         // pedir al scheduler que me diga cual servidor tine disponible ese servicio
         // y establecer la conexión con ese servidor y pasarle los parametros con JSON
         
         // RequestServiceMsg sCode = new RequestServiceMsg(0,servicio);
         RequestServiceMsg requestServiceMsg = new RequestServiceMsg(0,"wordcount");
         Gson gson = new Gson();
         String json = gson.toJson(requestServiceMsg);

        try {
            direccion=InetAddress.getByName("localhost");
            servidor=new Socket(direccion,1024);
            DataInputStream datos= new DataInputStream(servidor.getInputStream());
            System.out.println(datos.readLine());
            servidor.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}