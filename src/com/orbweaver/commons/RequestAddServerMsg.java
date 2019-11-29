package com.orbweaver.commons;


public class RequestAddServerMsg {
    private int code = Constants.CODE_REQUEST_ADD_SERVER;
    private ServerInfo server;

    public RequestAddServerMsg(ServerInfo server) {
        this.server = server;
    }

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        this.server = server;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}