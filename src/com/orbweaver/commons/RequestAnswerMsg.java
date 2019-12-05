package com.orbweaver.commons;

import static com.orbweaver.commons.Constants.STATUS_ERROR_REQUEST;
import static com.orbweaver.commons.Constants.STATUS_SUCCESS_REQUEST;

/**
 * Simple clase representa un estado existoso como respuesta a un mensaje
 */
public class RequestAnswerMsg {
    private int status = Constants.STATUS_SUCCESS_REQUEST; // Nombre del servicio a pedir
    private int code = -1 ; // Codigo de error

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public boolean isSuccess(){
        return this.status == STATUS_SUCCESS_REQUEST;
    }
    public boolean isError(){
        return this.status == STATUS_ERROR_REQUEST;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
