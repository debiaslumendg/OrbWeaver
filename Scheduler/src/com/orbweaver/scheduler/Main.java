package com.orbweaver.scheduler;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.Date;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub	

		SchedulerServer server = new SchedulerServer(1025);
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
