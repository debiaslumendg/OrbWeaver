package com.orbweaver.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public interface ServiceInterfaz {
    String getName();

    void handleClient(Socket socket,DataInputStream dataInputStream, DataOutputStream dataOutputStream);
}
