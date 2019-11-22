package com.orbweaver.client;

// import com.orbweaver.commons;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws Exception {
        
        // IP donde se hará la conexión
        InetAddress direccion;
        // Canal de comunicación con el servidor
        Socket servidor;
        String servicio, parametro;

        if (args.length < 1) {
            System.out.println("Usage:<Servicio a ejeutar> <Parámetro del servicio>");
            System.out.println();
            System.exit(-1);
        }

        servicio = args[0];
        parametro = args[1];
        
        // pedir al scheduler que me diga cual servidor tine disponible ese servicio
        // y establecer la conexión con ese servidor y pasarle los parametros con JSON
        
        // RequestServiceMsg sCode = new RequestServiceMsg(0,servicio);
        RequestServiceMsg sCode = new RequestServiceMsg(0,"wordcount");
        Gson gson = new Gson();
        String json = gson.toJson(sID);

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