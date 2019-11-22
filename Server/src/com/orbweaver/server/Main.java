package com.orbweaver.server;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub	
		
		Server server = new Server(1024);
		new Thread(server).start();

		try {
		    Thread.sleep(60 * 1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
		
		System.out.println("Stopping Server");
		server.stop();
		
	}

}
