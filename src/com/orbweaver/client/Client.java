package com.orbweaver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.orbweaver.commons.*;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static com.orbweaver.commons.Constants.STATUS_ERROR_REQUEST;
import static com.orbweaver.commons.Constants.STATUS_SUCCESS_REQUEST;

public class Client implements Runnable {

    protected int portScheduler;
    protected String addressScheduler;

    protected String serviceName;
    /** Cuando se realiza la peticion de una ejecucion al servidor si esta falla  por error del request_id se prueba a
     *  ejecutar de nuevo la request.
     */
    private boolean mTryAgainRequest;

    /**
     * N intentos en ejecutar la request
     */
    private int ntries = 0;
    private OnServiceArgumentsToServer onServiceArgumentsToServer;

    public Client(String serviceName,int portScheduler, String schedulerAddress) {
        this.serviceName = serviceName;
        this.portScheduler = portScheduler;
        this.addressScheduler = schedulerAddress;
    }

    /**
     * Se comunica con el Scheduler para pedirle la ejecución de un servicio
     * @return clase request respuesta del scheduler
     */
    protected RequestServiceAnswerMsg getRequestScheduler(){

        RequestServiceAnswerMsg requestServiceAnswerMsg = null;
        System.out.format("Connecting to Scheduler(%s,%d)\n",this.addressScheduler,this.portScheduler);

        Socket socketScheduler = null;

        try {
            socketScheduler = new Socket(this.addressScheduler,this.portScheduler);
            System.out.println("Connected to Scheduler");
        } catch (IOException e) {
            // TODO: Puede time out
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }


        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        JsonObject jsonObjectMessage;
        String json;

        try {
            dataOutputStream    = new DataOutputStream(socketScheduler.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socketScheduler.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Cannot open connection to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }

        RequestServiceMsg requestServiceMsg = new RequestServiceMsg(serviceName);
        json = gson.toJson(requestServiceMsg);

        System.out.println("Sending " + json + " to the Scheduler");

        try {
            dataOutputStream.writeUTF(json);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }

        try {
            // Obtenemos el contenido del mensaje del scheduler
            json = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot read answer from Scheduler ( %s , %d)",
                            this.addressScheduler,this.portScheduler)
                    , e);
        }


        requestServiceAnswerMsg = gson.fromJson(json, RequestServiceAnswerMsg.class);

        switch (requestServiceAnswerMsg.getStatus()){
            case Constants.STATUS_SUCCESS_REQUEST:
                System.out.println("[Server] Got from Scheduler: ");
                String requestId = requestServiceAnswerMsg.getRequestId();
                System.out.println("[Server] \trequestId: " + requestId);
                ServerInfo serverInfo = requestServiceAnswerMsg.getServerInfo();
                System.out.println("[Server] \tserverInfo: " + serverInfo);

                break;
            case STATUS_ERROR_REQUEST:
                Util.mostrarErrorPrint(requestServiceAnswerMsg.getCode());
                break;
        }

        try {
            dataOutputStream.close();
            dataInputStream.close();
            socketScheduler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return requestServiceAnswerMsg;
    }
    /**
     * Se comunica con el Servidor para enviarle la request de servicio
     *
     * TODO: Falta manejo de errores así como en todo el programa por eso no unifico todavía el código parecido para los
     *      exception ya que cada exception se puede manejar distinto dependiendo de la clase y mensaje
     */
    private void sendRequestToServer(ServerInfo serverInfo,String requestId){

        Socket socket;

        try {
            socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }

        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;

        try {
            dataOutputStream    = new DataOutputStream(socket.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Client] Error: Cannot open connection to Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }


        RequestServiceMsg requestServiceMsg = new RequestServiceMsg("wordcount");
        requestServiceMsg.setIdRequest(requestId);
        content = gson.toJson(requestServiceMsg);

        System.out.format("[Client] Sending " + content + " to the Server (%s,%s, %d)\n",
                serverInfo.getName(),serverInfo.getAddress(),serverInfo.getPort());

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Client] Error: Cannot read JSON from Server ( %s , %d)",
                            serverInfo.getAddress(), serverInfo.getPort()), e);
        }


        System.out.println("[Client] Received from Server" + content);

        RequestAnswerMsg requestAnswerMsg = gson.fromJson(content, RequestAnswerMsg.class);

        switch (requestAnswerMsg.getStatus()){
            case Constants.STATUS_SUCCESS_REQUEST:
                if(onServiceArgumentsToServer != null){
                    onServiceArgumentsToServer.onServiceArgumentsToServer(socket,dataInputStream,dataOutputStream);
                }
                break;
            case STATUS_ERROR_REQUEST:
                Util.mostrarErrorPrint(requestAnswerMsg.getCode());
                mTryAgainRequest = true;
                break;
        }

        try {
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        RequestServiceAnswerMsg answerScheduler;
        mTryAgainRequest = true;
        ntries = 0;
        while(mTryAgainRequest && ntries <3) {
            mTryAgainRequest = false;
            answerScheduler = getRequestScheduler();
            if ( answerScheduler.getStatus() == STATUS_SUCCESS_REQUEST) {
                sendRequestToServer(answerScheduler.getServerInfo(),answerScheduler.getRequestId());
            }
            ntries ++;
        }
    }

    public void setOnServiceArgumentsToServer(OnServiceArgumentsToServer onServiceArgumentsToServer) {
        this.onServiceArgumentsToServer = onServiceArgumentsToServer;
    }

    public OnServiceArgumentsToServer getOnServiceArgumentsToServer() {
        return onServiceArgumentsToServer;
    }
}
