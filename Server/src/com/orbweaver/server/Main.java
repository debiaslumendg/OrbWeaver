package com.orbweaver.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.Date;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub	
		
		Server server = new Server(1024);
		new Thread(server).start();

		try {
		    Thread.sleep(20 * 1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
		
		System.out.println("Stopping Server");
		server.stop();
		
	}

}
