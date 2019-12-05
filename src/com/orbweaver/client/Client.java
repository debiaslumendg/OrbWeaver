package com.orbweaver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.orbweaver.commons.*;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static com.orbweaver.commons.Constants.STATUS_ERROR_REQUEST;
import static com.orbweaver.commons.Constants.STATUS_SUCCESS_REQUEST;

public class Client implements Runnable {

    private int portScheduler;
    private String addressScheduler;

    private String serviceName;
    /** Cuando se realiza la peticion de una ejecucion al servidor si esta falla  por error del request_id se prueba a
     *  ejecutar de nuevo la request.
     */
    //private boolean mTryAgainRequest;

    private OnServiceArgumentsToServer onServiceArgumentsToServer;

    public Client(String serviceName,int portScheduler, String schedulerAddress) {
        this.serviceName = serviceName;
        this.portScheduler = portScheduler;
        this.addressScheduler = schedulerAddress;
    }

    public RequestServiceAnswerMsg getRequestScheduler() {
        return getRequestScheduler("");
    }

    /**
     * Se comunica con el Scheduler para pedirle la ejecución de un servicio
     * @return clase request respuesta del scheduler
     */
    public RequestServiceAnswerMsg getRequestScheduler(String oldRequestId){

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
        if(StringUtils.isNotEmpty(oldRequestId)){
            requestServiceMsg.setIdRequest(oldRequestId);
        }

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
     * @return
     */
    private boolean sendRequestToServer(ServerInfo serverInfo, String requestId){

        Socket socket;

        try {
            socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        } catch (IOException e) {
            System.out.format("Error: Cannot connect to Server ( %s , %d)\n",serverInfo.getAddress(), serverInfo.getPort());
            return false;
        }

        Gson gson = new Gson();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;

        try {
            dataOutputStream    = new DataOutputStream(socket.getOutputStream());
            dataInputStream     = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            System.out.format("[Client] Error: Cannot open connection to Server ( %s , %d)\n",
                    serverInfo.getAddress(), serverInfo.getPort());
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            return false;
        }


        RequestServiceMsg requestServiceMsg = new RequestServiceMsg(serviceName);
        requestServiceMsg.setIdRequest(requestId);
        content = gson.toJson(requestServiceMsg);

        System.out.format("[Client] Sending " + content + " to the Server (%s,%s, %d)\n",
                serverInfo.getName(),serverInfo.getAddress(),serverInfo.getPort());

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            System.out.format("[Client] Error: Cannot write JSON to Server ( %s , %d)\n",
                    serverInfo.getAddress(), serverInfo.getPort());
            try {
                dataOutputStream.close();
                dataInputStream.close();
                socket.close();
            } catch (IOException ignored) {
            }
            return false;
        }

        // Aquí el servidor actualizó el estado de la request en el scheduler
        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            System.out.format("[Client] Error: Cannot read JSON from Server ( %s , %d)\n",
                    serverInfo.getAddress(), serverInfo.getPort());
            try {
                dataOutputStream.close();
                dataInputStream.close();
                socket.close();
            } catch (IOException ignored) {
            }
            return false;
        }

        System.out.println("[Client] Received from Server" + content);

        RequestAnswerMsg requestAnswerMsg = gson.fromJson(content, RequestAnswerMsg.class);

        if (requestAnswerMsg.getStatus() == STATUS_SUCCESS_REQUEST) {
            if (onServiceArgumentsToServer != null) {
                onServiceArgumentsToServer.onServiceArgumentsToServer(socket, dataInputStream, dataOutputStream);
            }
        }

        try {
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(requestAnswerMsg.getStatus() == STATUS_ERROR_REQUEST){
            Util.mostrarErrorPrint(requestAnswerMsg.getCode());
            return false;
        }
        return true;
    }

    @Override
    public void run() {

        RequestServiceAnswerMsg answerScheduler;
        /**
         * N intentos en ejecutar la request
         */
        int ntries = 0;

        answerScheduler = getRequestScheduler();

        if(answerScheduler.isError()){
            return;
        }

        while(ntries <3) {

            if(sendRequestToServer(answerScheduler.getServerInfo(),answerScheduler.getRequestId())) break; ;

            answerScheduler = getRequestScheduler(answerScheduler.getRequestId());
            if(answerScheduler.isError()){
                break;
            }
            ntries++;
        }

        if(ntries >= 3){
            System.out.println("Error ejecutando el servicio!");
        }
    }

    public void setOnServiceArgumentsToServer(OnServiceArgumentsToServer onServiceArgumentsToServer) {
        this.onServiceArgumentsToServer = onServiceArgumentsToServer;
    }

    public OnServiceArgumentsToServer getOnServiceArgumentsToServer() {
        return onServiceArgumentsToServer;
    }
}
