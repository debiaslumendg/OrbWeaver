package com.orbweaver.commons;

public class RequestServiceMsg {

    public String name;
    public int code = -1;

    public RequestServiceMsg (int code, String name){
        this.code = code;
        this.name = name;
    }
}