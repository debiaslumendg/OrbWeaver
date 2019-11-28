package com.orbweaver.commons;


public class RequestAddServerMsg {
    public int code = 1;
    private ServerObject server;

    public RequestAddServerMsg(ServerObject server) {
        this.server = server;
    }

    public ServerObject getServer() {
        return server;
    }

    public void setServer(ServerObject server) {
        this.server = server;
    }
}