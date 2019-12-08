package com.orbweaver.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public interface ServiceInterfaz {
    String getName();

    boolean handleClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream);
}
