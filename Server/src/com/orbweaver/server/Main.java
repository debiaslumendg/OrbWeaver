package com.orbweaver.server;

public class Main {

	public static void main(String[] args) {
		
		// Leer de args puerto e IP donde se se conectará el servidor
		int port = 1025;

		Server server = new Server(port);
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
