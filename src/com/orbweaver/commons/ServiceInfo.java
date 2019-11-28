package com.orbweaver.commons;

public class ServiceInfo {

    private int id;
    private String name;

    public ServiceInfo(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    @Override
    public String toString() {
        return String.format("ServiceInfo (id = %s ,name = %s) ",id,name);
    }

    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return  id;
    }
}
