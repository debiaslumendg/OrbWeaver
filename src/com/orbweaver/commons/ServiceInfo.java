package com.orbweaver.commons;

public class ServiceInfo {

    private String name;

    public ServiceInfo(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    @Override
    public String toString() {
        return String.format("ServiceInfo (name = %s) ",name);
    }
}
