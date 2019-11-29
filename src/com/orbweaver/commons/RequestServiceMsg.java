package com.orbweaver.commons;

/**
 * Representa un mensaje de peticion de un servicio por un cliente
 */
public class RequestServiceMsg {

    private String name; // Nombre del servicio a pedir
    private int code = Constants.CODE_REQUEST_EXEC_SERVICE; // Código del mensaje

    public RequestServiceMsg (String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}