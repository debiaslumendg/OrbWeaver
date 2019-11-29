/**
 *
 * Este es el hilo que se inicia cuando el Servidor es elegido para ser Scheduler.
 *
 * Atiende :
 * 		Solicitudes de requerimientos de clientes.
 * 		Solicitudes de agregar al grupo de servidores.
 * 		y ..
 *
 *
 *  Autores:
 *      Natascha Gamboa      12-11250
 * 	    Manuel  Gonzalez    11-10390
 * 	    Pedro   Perez       10-10574
 */

package com.orbweaver.scheduler;

import com.orbweaver.commons.Constants;
import com.orbweaver.commons.ServerObject;
import com.orbweaver.commons.ServiceInfo;
import com.orbweaver.scheduler.tables.RequestServer;
import com.orbweaver.server.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Scheduler implements Runnable{

	/** Puerto en el que corre el Scheduler , esperando mensajes.*/
	private int          serverPort   = Constants.DEFAULT_SCHEDULER_PORT;

	/** Socket servidor , esperando clientes los cuales pueden ser programas clientes o servidores.*/
	private ServerSocket serverSocket = null;

	/** Indica si se ha detenido el Scheduler*/
	private boolean      isStopped    = false;

	/**Apuntador al Servidor que inició el hilo*/
    private Server parentServer = null;

    /**Lista de requests que está manejando el Scheduler*/
	private HashMap<Integer, RequestServer> mRequestServer = new HashMap<>() ;

	/**
	 * Crea una instancia del Scheduler
	 * @param parentServer Servidor que inició el Scheduler
	 * @param port Puerto en el que correrá el Scheduler
	 */
	public  Scheduler(Server parentServer , int port){
	    this.parentServer = parentServer;
		serverPort = port;
	}

	/**
	 * Espera a los clientes y cuando recibe uno crea un hilo para atenderlo
	 */
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

	/**
	 *  Detiene al scheduler y cierra el socket
	 */
	public synchronized void stop(){
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}

	/**
	 * Función principal ejecutada por el hilo.
	 *
	 * Inicializa el socket donde recibirá a los clientes.
	 * Espera los mensajes
	 * Cierra el socket cuando termina -- Terminado por el usuario ctrl-z (TODO:Falta)
	 */
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

	/**
	 * Agrega un servidor al grupo.
	 * Se comunica con todos los demás servidores en el grupo comunicandoles su llegada
	 * @param server Objeto conteniendo información del nuevo servidor
	 * @return ID del nuevo servidor
	 */
	public int addServer(ServerObject server) {
		return parentServer.addServer(server);
	}


	/**
	 * Obtiene todos los servicios actuales que tiene el Servidor.
	 * NO SON los servicios que ofrece el servidor sino los del grupo completo que él tiene, que deberían ser los actuales
	 * porque cuando se agrega un servidor al grupo se envía en su información sus servicios ofrecidos.
	 * @return
	 */
	public ArrayList<ServiceInfo> getServices() {
		return parentServer.getServices();
	}

	/**
	 * Lista de servidores en el grupo. Actualizada cuando se agrega un servidor al grupo.
	 * @return
	 */
	public ArrayList<ServerObject> getServers() {
		return parentServer.getServers();
	}
}
