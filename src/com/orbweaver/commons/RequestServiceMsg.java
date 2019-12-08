package com.orbweaver.commons;

/**
 * Representa un mensaje de peticion de un servicio por un cliente
 */
public class RequestServiceMsg {

    private int code = Constants.CODE_MESSAGE_EXEC_SERVICE; // Código del mensaje
    private String name; // Nombre del servicio a pedir
    private String id_request; // ID del request

    public RequestServiceMsg (String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdRequest() {
        return id_request;
    }

    public void setIdRequest(String idRequest) {
        this.id_request = idRequest;
    }
}