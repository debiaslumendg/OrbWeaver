package com.orbweaver.commons;


import java.util.ArrayList;

public class RequestAddServerAnswerMsg {
    public int code = Constants.CODE_SUCCESS_REQUEST;
    private int server_id;

    private ArrayList<ServiceInfo> services;
    private ArrayList<ServerObject> servers;

    public RequestAddServerAnswerMsg(int server_id, ArrayList<ServiceInfo> services, ArrayList<ServerObject> servers) {
        this.server_id = server_id;
        this.services = services;
        this.servers = servers;
    }


    public int getServer_id() {
        return server_id;
    }

    public void setServer_id(int server_id) {
        this.server_id = server_id;
    }

    public ArrayList<ServiceInfo> getServices() {
        return services;
    }

    public void setServices(ArrayList<ServiceInfo> services) {
        this.services = services;
    }

    public ArrayList<ServerObject> getServers() {
        return servers;
    }

    public void setServers(ArrayList<ServerObject> servers) {
        this.servers = servers;
    }
}