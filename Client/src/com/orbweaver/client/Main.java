package com.orbweaver.client;

import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws Exception {
        
        InetAddress direccion;
        Socket servidor;
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