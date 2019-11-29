package com.orbweaver.commons;

import java.util.List;

public class ServerInfo {

    private int id;
    private String name;
    private String address;
    private int port;
    private List<ServiceInfo> services;


    public ServerInfo(String name, String address, int port, List<ServiceInfo> services) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.services = services;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return "Server( id= " + getId()   +  ", name= " + getName() + " , address = "  + getAddress() +
                ", port = "   + getPort() +  ", services = " + getServices() + " ) ";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}