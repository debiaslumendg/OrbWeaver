package com.orbweaver.commons;


import java.util.ArrayList;

public class RequestAddServerAnswerMsg {
    public int status = Constants.STATUS_SUCCESS_REQUEST;
    private int server_id;

    private ArrayList<ServiceInfo> services;
    private ArrayList<ServerInfo> servers;

    public RequestAddServerAnswerMsg(int server_id, ArrayList<ServiceInfo> services, ArrayList<ServerInfo> servers) {
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

    public ArrayList<ServerInfo> getServers() {
        return servers;
    }

    public void setServers(ArrayList<ServerInfo> servers) {
        this.servers = servers;
    }
}