package com.orbweaver.commons;

/**
 * Simple clase representa un estado existoso como respuesta a un mensaje
 */
public class RequestAnswerMsg {
    private int status = Constants.STATUS_SUCCESS_REQUEST; // Nombre del servicio a pedir
    private int code = -1 ;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
