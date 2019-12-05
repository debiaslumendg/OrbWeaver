package com.orbweaver.server;

import com.google.gson.*;
import com.orbweaver.commons.*;
import com.orbweaver.scheduler.tables.RequestServer;

import java.io.*;
import java.net.Socket;

import static com.orbweaver.commons.Constants.STATUS_ERROR_REQUEST;
import static com.orbweaver.commons.Constants.STATUS_SUCCESS_REQUEST;

public class ServerWorker implements Runnable{

    protected Socket clientSocket = null;
    protected Server server   = null;

    public ServerWorker(Server server,Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.server   = server;
    }

    public void run() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;
        JsonObject jsonObjectMessage;

        // Obtenemos el contenido del mensaje del cliente
        try{
            dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot open connection to Client ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostAddress(),this.clientSocket.getPort())
                    , e);
        }


        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Client ( %s , %d)",
                            this.clientSocket.getInetAddress().getHostName(),this.clientSocket.getPort())
                    , e);
        }


        // Parseamos el mensaje a JSON
        try {
            jsonObjectMessage = new JsonParser().parse(content).getAsJsonObject();
        }catch (JsonSyntaxException e){
            System.out.println("[Server] Error : incorrect message sent by client");
            System.out.println("[Server] Message: " + content);

            return ;
        }

        System.out.println("[Server] " + jsonObjectMessage);

        int code = jsonObjectMessage.get("code").getAsInt();

        switch (code){
            case Constants.CODE_REQUEST_ADD_SERVER:
                RequestAddServerMsg requestAddServerMsg = gson.fromJson(content, RequestAddServerMsg.class);

                server.addServer(requestAddServerMsg.getServer());

                break;
            case Constants.CODE_REQUEST_EXEC_SERVICE:
                RequestServiceMsg requestServiceMsg = gson.fromJson(content, RequestServiceMsg.class);

                RequestAnswerMsg validateRequestAnswer = updateStatusRequestScheduler(
                        requestServiceMsg.getIdRequest(),
                        RequestServer.StatusRequest.RUNNING
                );

                // Enviamos el mensaje de respuesta -- errror -success
                content = gson.toJson(validateRequestAnswer);
                try {
                    dataOutputStream.writeUTF(content);
                } catch (IOException e) {
                    throw new RuntimeException(
                            String.format("Error: Cannot write JSON to Server ( %s , %d)",
                                    clientSocket.getInetAddress().getHostName(),clientSocket.getPort())
                            , e);
                }
                // En caso de que sea exitosa la petición, llamamos a la clase encargada de manejarla con el socket
                // del cliente
                if(validateRequestAnswer.getStatus() == STATUS_SUCCESS_REQUEST) {
                    ServiceInterfaz service = server.getServiceExec(requestServiceMsg.getName());
                    service.handleClient(clientSocket, dataInputStream, dataOutputStream);

                    updateStatusRequestScheduler(requestServiceMsg.getIdRequest(), RequestServer.StatusRequest.DONE);
                }
                break;
        }

        //long time = System.currentTimeMillis();
        //System.out.println("Request processed: " + time);

        try{
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }

    /**
     * Se comunica con el scheduler para actualizar el estado de una request
     * Scheduler responde Error:
     *  - Si la request es inválida
     *  - Si la request ya está resuelta
     *  Sino:
     *  - Si la request es válida , establece el servidor como ejecutandola
     */
    private RequestAnswerMsg updateStatusRequestScheduler(String idRequest, RequestServer.StatusRequest newStatus) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Socket socket;

        try {
            socket = new Socket(this.server.geteCoordinatorAddress(),this.server.getCoordinatorPort());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Scheduler ( %s , %d)",
                            this.server.geteCoordinatorAddress(), this.server.getCoordinatorPort()), e);
        }

        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;

        String content;


        // Obtenemos el contenido del mensaje del cliente
        try{
            dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot connect to Scheduler ( %s , %d)",
                            this.server.geteCoordinatorAddress(), this.server.getCoordinatorPort()), e);
        }

        RequestUpdateRequestToScheduler requestUpdateRequestToScheduler = new RequestUpdateRequestToScheduler(
                this.server.getId(),
                idRequest,
                newStatus
        );
        content = gson.toJson(requestUpdateRequestToScheduler);
        System.out.println("[Server] Sending to Scheduler to Validate: " + content);

        try {
            dataOutputStream.writeUTF(content);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error: Cannot write JSON to Server ( %s , %d)",
                            socket.getInetAddress().getHostName(),socket.getPort())
                    , e);
        }

        try {
            content = dataInputStream.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("[Server] Error: Cannot read JSON from Scheduler ( %s , %d)",
                            this.server.geteCoordinatorAddress(), this.server.getCoordinatorPort()), e);
        }

        System.out.println("[Server] Received from Scheduler Validation: " + content);
        RequestAnswerMsg answer = gson.fromJson(content, RequestAnswerMsg.class);

        try{
            dataInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }

        return answer;

    }

}