package com.orbweaver.commons;


import com.orbweaver.server.RequestInfo;

import java.util.ArrayList;

public class RequestAddServerAnswerMsg {
    public int status = Constants.STATUS_SUCCESS_REQUEST;
    private int server_id;

    private ArrayList<ServerInfo> servers;
    private ArrayList<RequestInfo> requests;

    public RequestAddServerAnswerMsg(int server_id, ArrayList<ServerInfo> servers, ArrayList<RequestInfo> requests) {
        this.server_id = server_id;
        this.servers = servers;
        this.requests = requests;
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

    public ArrayList<RequestInfo> getRequests() {
        return requests;
    }

    public void setRequests(ArrayList<RequestInfo> requests) {
        this.requests = requests;
    }
}