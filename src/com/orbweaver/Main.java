package com.orbweaver;

import com.orbweaver.client.Client;
import com.orbweaver.commons.Service;
import com.orbweaver.server.Server;

public class Main {

	public static void main(String[] args) {

		if (args.length < 1) {
			System.out.println("Usage:<client/server> <Servicio a ejeutar> <Parámetro del servicio>");
			System.out.println();
			System.exit(-1);
		}
		
		Service serv = new Service();
		serv.setId(1L);

		System.out.print(serv.toJson());

		switch (args[0]) {
		case "server":
			Server server = new Server(1024,"1029",1202,1231,false);
			server.run();
			break;
		case "client":
			try {
				Client.client(args[1],args[2]);
			} catch (Exception e) {
				e.printStackTrace();
			}
				break;
		}

	}

}
