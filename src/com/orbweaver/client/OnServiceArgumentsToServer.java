package com.orbweaver.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;


/**
 * Esta interfaz representa una especialización de un cliente.
 * Cuando un Cliente General (Clase Client) , hace contacto con el servidor, llama a una implementación de esta interfaz  -
 *      para que maneje la comunicación especializada con el servidor que depende del servicio solicitado
 *
 */
public interface OnServiceArgumentsToServer {
    public void onServiceArgumentsToServer(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream);
}
