package com.orbweaver.commons;


public class RequestAddServerMsg {
    private int code = Constants.CODE_REQUEST_ADD_SERVER;
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

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}