package com.orbweaver.commons;


import java.util.ArrayList;

public class RequestAddServerAnswerMsg {
    public int status = Constants.STATUS_SUCCESS_REQUEST;
    private int server_id;

    private ArrayList<ServerInfo> servers;

    public RequestAddServerAnswerMsg(int server_id,ArrayList<ServerInfo> servers) {
        this.server_id = server_id;
        this.servers = servers;
    }


    public int getServer_id() {
        return server_id;
    }

    public void setServer_id(int server_id) {
        this.server_id = server_id;
    }

    public ArrayList<ServerInfo> getServers() {
        return servers;
    }

    public void setServers(ArrayList<ServerInfo> servers) {
        this.servers = servers;
    }
}