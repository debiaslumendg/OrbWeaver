package com.orbweaver.commons;

import java.util.List;

public class ServerInfo {

    private int id;
    private String address;
    private int port;
    private List<ServiceInfo> services;


    public ServerInfo(int port, List<ServiceInfo> services) {
        this.port = port;
        this.services = services;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<ServiceInfo> getServices() {
        return services;
    }

    public void setServices(List<ServiceInfo> services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return "Server( id= " + getId()   +  ", address = "  + getAddress() +
                ", port = "   + getPort() +  ", services = " + getServices() + " ) ";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
