package com.orbweaver.scheduler;

import com.orbweaver.commons.Constants;
import com.orbweaver.commons.ServerObject;
import com.orbweaver.scheduler.tables.RequestServer;
import com.orbweaver.server.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Scheduler implements Runnable{

	private int          serverPort   = Constants.DEFAULT_SCHEDULER_PORT;
	private ServerSocket serverSocket = null;
	private boolean      isStopped    = false;
	//private Thread       runningThread= null;

    private Server parentServer = null;

	private HashMap<Integer, RequestServer> mRequestServer = new HashMap<>() ;


	public  Scheduler(Server parentServer , int port){
	    this.parentServer = parentServer;
		serverPort = port;
	}

	private void waitForMessages(){
		while(! isStopped){

			Socket clientSocket = null;
            System.out.println("[Scheduler] Waiting for a client ...");
			try {
				clientSocket = this.serverSocket.accept();
			} catch (IOException e) {
				throw new RuntimeException("[Scheduler] Error accepting client connection", e);
			}

            InetAddress inetAddress = clientSocket.getInetAddress();
            System.out.format("[Scheduler] Accepted client %s \n",inetAddress.getHostAddress());
			new Thread(new SchedulerWorker(clientSocket,this)).start();
		}
	}
	public synchronized void stop(){
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}

	public void run(){

		//readFileJsonServices();
		openServerSocket();

		waitForMessages();

		System.out.println("[Scheduler] Scheduler Stopped.") ;
		stop();

	}


    /**
	 * Inicializa el Socket-donde se esperaran las peticiones de clientes - Scheduler
	 */
	private void openServerSocket() {
		try {
			this.serverSocket = new ServerSocket(this.serverPort);
			System.out.format("[Scheduler] Running scheduler on port: %d\n",this.serverPort);
		} catch (IOException e) {
			throw new RuntimeException(String.format("[Scheduler] Error: Cannot open port %d",this.serverPort), e);
		}
	}

	/**
	 *  Lee de un archivo externo en formato JSON los servicios definidos.
	public void readFileJsonServices(){

		JsonArray jsonArrayServices = null;
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(Paths.get("services.json"));
			String content = new String(bytes);
			jsonArrayServices = new JsonParser().parse(content).getAsJsonArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(jsonArrayServices != null){
			String nameService;
			for (int i = 0; i < jsonArrayServices.size(); i++) {
				nameService = jsonArrayServices.get(i).getAsJsonObject().get("name").getAsString();
				mServicios.put(0,new ServicioInfo(0,nameService)) ;
			}
		}
	}
	 */

    public void addServer(ServerObject server) {
        parentServer.addServer(server);
    }
}
