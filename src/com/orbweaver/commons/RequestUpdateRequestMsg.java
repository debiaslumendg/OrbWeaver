package com.orbweaver.commons;

import com.orbweaver.server.RequestInfo;


/**
 * Representa el mensaje que le envía el servidor al scheduler para verificar una request de servicio
 * ID que le ha pasado el usuario
 */
public class RequestUpdateRequestMsg {
    private int code = Constants.CODE_REQUEST_UPDATE_REQUEST; // Codigo de la accion al scheduler
    private int server_id = -1 ; // ID del servidor validando la request
    private RequestInfo.StatusRequest new_status; // ID del servidor validando la request
    private String request_id; // ID De la request a validar

    public RequestUpdateRequestMsg(int server_id, String request_id, RequestInfo.StatusRequest new_status) {
        this.server_id = server_id;
        this.request_id = request_id;
        this.new_status = new_status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getServer_id() {
        return server_id;
    }

    public void setServer_id(int server_id) {
        this.server_id = server_id;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public RequestInfo.StatusRequest getNew_status() {
        return new_status;
    }

    public void setNew_status(RequestInfo.StatusRequest new_status) {
        this.new_status = new_status;
    }
}
