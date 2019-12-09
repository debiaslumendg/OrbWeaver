package com.orbweaver.server.scheduler;

import com.google.gson.Gson;
import com.orbweaver.commons.*;
import com.orbweaver.server.RequestInfo;
import com.orbweaver.server.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.UUID;
import java.util.Vector;

import static com.orbweaver.commons.Constants.*;

/**
 *
 * Este es el hilo que se inicia cuando el Servidor es elegido para ser Scheduler.
 *
 * Atiende solicitudes como un servidor y crea un hilo para menajarlas.
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
public class Scheduler implements Runnable{

	/** Puerto en el que corre el Scheduler , esperando mensajes.*/
	private int          serverPort   = Constants.DEFAULT_SCHEDULER_PORT;

	/** Socket servidor , esperando clientes los cuales pueden ser programas clientes o servidores.*/
	private ServerSocket serverSocket = null;

	/** Indica si se ha detenido el Scheduler*/
	private boolean      isStopped    = false;

	/**Apuntador al Servidor que inició el hilo*/
    private Server parentServer = null;

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
	 * Lista de servidores en el grupo. Actualizada cuando se agrega un servidor al grupo.
	 * @return servidores en el grupo
	 */
	public ArrayList<ServerInfo> getServers() {
		return parentServer.getServers();
	}

	/**
	 * Crea la respuesta satisfactoria al cliente de un requerimiento de servicio
	 * @param serviceName Nombre único del servicio
	 * @return clase request
	 */
	public RequestServiceAnswerMsg createRequestService(String serviceName) {

		RequestServiceAnswerMsg requestServiceAnswerMsg = new RequestServiceAnswerMsg();

		ArrayList<ServerInfo> allServersService = parentServer.getAllServersByServiceName(serviceName);

		if( allServersService.size() == 0) {
			requestServiceAnswerMsg.setStatus(STATUS_ERROR_REQUEST);
			requestServiceAnswerMsg.setCode(CODE_ERROR_SOLICITED_SERVICE_NOT_SERVER_FOUND);
			return requestServiceAnswerMsg;
		}

		ArrayList<ServerInfo> orderedServers = ordServersByLoad(allServersService);

		ServerInfo server = orderedServers.get(0); // El que tenga menor carga


		RequestInfo requestInfo = new RequestInfo(UUID.randomUUID().toString(), server.getId());
		getRequests().add(requestInfo);

		requestServiceAnswerMsg.setServerInfo(server);
		requestServiceAnswerMsg.setRequestId(requestInfo.getId());
		return requestServiceAnswerMsg;
	}
	public ArrayList<RequestInfo> getRequests(){
		return this.parentServer.getRequests();
	}

    /**
     * Envia un mensaje al grupo
     * - Nuevo servidor
     * - Request actualizado
     * --- El unico que deberia llamar esta funcion es el scheduler. syncronized deberia hacer los mensajes ordenados
     */
    public synchronized void sendMessageToGroup(String message) {

        Vector<Integer> serversToRemove = new Vector<>();

        ListIterator<ServerInfo> iter = this.getServers().listIterator();
        ServerInfo serverInfo;
        while(iter.hasNext()) {
            serverInfo = iter.next();

            // Envia el mensaje a todos menos a él mismo
            if(serverInfo.getId() != this.parentServer.getId()) {
                if(!this.parentServer.sendMessageToServer(message, serverInfo)){
                    serversToRemove.add(serverInfo.getId());
                    iter.remove();
                }
            }
        }

        if(serversToRemove.size() > 0){
            sendMessageToGroup(String.format("{\"code\":%d,\"id_servers\":%s}",
                    Constants.CODE_MESSAGE_REMOVE_SERVER,
                    new Gson().toJson(serversToRemove.toArray())));
        }
    }
    /**
     * Agrega un servidor a su lista de miembros.
     * -    Para agregarse un servidor al grupo se debe comunicar con el coordinador, para nuestro proyecto es el mismo Scheduler.
     * -    El scheduler lo agrega a su lista de miembros interna y le asigna un ID a él y a sus servicios nuevos que el scheduler no conozca.
     * -    El scheduler después de agregarlo a su lista interna le envía un mensaje a todos los miembros del grupo, con
     *      la información del nuevo miembro del grupo para que se actualizen.
     *      Es necesario que todos los miembros sepan quienes son el resto, por si:
     *          * Ocurre un error y el scheduler y el scheduler back up mueren, cualquier puede tomar el lugar de scheduler.
     *              Por lo que necesita la lista de miembros para seleccionar entre ellos el nuevo scheduler.
     *
     * @param requestAddServerMsg Informacion del nuevo servidor a agregar
     * @return ID del nuevo servidor
     */
    public synchronized int addServer(RequestAddServerMsg requestAddServerMsg){

        // Se lee el id del anterior servidor para controlar la eliminación y agregación de servidores
        int newServerID;
        ServerInfo serverInfo = requestAddServerMsg.getServer();

        // TODO : Cambiar la forma en que se maneja la creacion de ids de los servidores
        ArrayList<ServerInfo> servers = this.getServers();
        newServerID = getNextServerID();
        serverInfo.setId(newServerID);
		setNextID(newServerID + 1);

        sendMessageToGroup(new Gson().toJson(requestAddServerMsg));

        servers.add(serverInfo);

        System.out.format("Added server ID=%d :\n\tAddress - (%s,%d)\n", newServerID, serverInfo.getAddress(), serverInfo.getPort());
        System.out.format("\tServices %s\n", serverInfo.getServices());

        return newServerID;
    }

	private void setNextID(int n) {
		this.parentServer.setNextServerID(n);
	}

	public boolean pingServer(ServerInfo server) {
		return this.parentServer.pingServer(server);
	}

	/**
	 * Obtiene el numero de request que un servidor está ejecutando (o se tiene planeado que ejecute)
	 * @param idServer ID del servidor
	 * @return numero de request asociadas al servidor
	 */
	public int getLoadForServerByID(int idServer) {

		int load = 0;
		for(RequestInfo requestInfo : this.parentServer.getRequests()){
			if(requestInfo.getIdServer() == idServer){
				load ++;
			}
		}
		return load;
	}

	/**
	 * Ordena una lista de servidores de quien tiene menos carga de trabajo a -> quien tiene más carga de trabajo
	 *
	 * Utiliza un algoritmo de burbuja
	 * @param servers array de servidor
	 * @return array de servidores ordenados
	 */
	private ArrayList<ServerInfo> ordServersByLoad(ArrayList<ServerInfo> servers) {

		int n = servers.size();

		/** Utilizo arrays primitivos para hacer el algoritmo más rápido*/
		int[] loads = new int[n];

		for(int i = 0; i < n ; i++){
			loads[i] = getLoadForServerByID(servers.get(i).getId());
		}

		boolean ordered = false;
		boolean changed = false;

		int i;
		int aux;
		ServerInfo auxS;
		while(!ordered){

			i = 0;
			changed = false;
			ordered = true;
			while (i < n - 1) {
				if (loads[i + 1] < loads[i]) {
					aux = loads[i];
					loads[i] = loads[i + 1];
					loads[i + 1] = aux;

					auxS = servers.get(i);
					servers.set(i,servers.get(i + 1));
					servers.set(i + 1,auxS);

					changed = true;

				}
				i ++ ;
			}

			if(changed) ordered = false;
		}


		return servers;
	}


	/**
	 * Esta funcion se encarga de decir si la request que fue pasada como argumento (lrecibida por el scheduler del servidor) -
	 * -es una request valida
	 * @param request
	 * @return
	 */
	public RequestAnswerMsg updateRequest(RequestUpdateRequestMsg request) {
		RequestAnswerMsg answer = new RequestAnswerMsg();

		RequestInfo r = this.parentServer.getRequestByID(request.getRequestID());
		if(r != null){
			if(r.getIdServer() == request.getServerID()){
				RequestInfo.StatusRequest newStatus = request.getNewStatus();
				if(newStatus == RequestInfo.StatusRequest.RUNNING &&
						(r.getStatus() == RequestInfo.StatusRequest.DONE ||
								r.getStatus() == RequestInfo.StatusRequest.RUNNING)) {
					answer.setStatus(STATUS_ERROR_REQUEST);
					answer.setCode(CODE_ERROR_INVALID_REQUEST_DUPLICATED);
				}else{
					answer.setStatus(STATUS_SUCCESS_REQUEST);
					r.setStatus(request.getNewStatus());

					sendMessageToGroup(new Gson().toJson(request));
				}
			}else{
				answer.setStatus(STATUS_ERROR_REQUEST);
				answer.setCode(CODE_ERROR_INVALID_REQUEST_UNAUTHORIZED_EXEC);
			}
		}else {
			answer.setStatus(STATUS_ERROR_REQUEST);
			answer.setCode(CODE_ERROR_INVALID_REQUEST_ID_NOT_FOUND);
		}
		return answer;
	}

	public RequestInfo getRequestByID(String requestId) {
		return this.parentServer.getRequestByID(requestId);
	}

	public ServerInfo getServerByID(int idServer) {
		return this.parentServer.getServerByID(idServer);
	}

	public void removeServerByID(int idServer) {
		this.parentServer.removeServerByID(idServer);
	}

	public void sendMessageRemoveServerToGroup(int idServer) {
		sendMessageToGroup(
				String.format("{\"code\":%d,\"id_servers\":[%d]}",
						Constants.CODE_MESSAGE_REMOVE_SERVER,
						idServer)
		);
	}

	public int getNextServerID() {
		return this.parentServer.getNextServerID();
	}
}
