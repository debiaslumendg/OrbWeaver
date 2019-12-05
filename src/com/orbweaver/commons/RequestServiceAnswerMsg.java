package com.orbweaver.commons;

import static com.orbweaver.commons.Constants.STATUS_ERROR_REQUEST;
import static com.orbweaver.commons.Constants.STATUS_SUCCESS_REQUEST;

/**
 * Representa un mensaje de peticion de un servicio por un cliente
 */
public class RequestServiceAnswerMsg {

    private int status = STATUS_SUCCESS_REQUEST; // Nombre del servicio a pedir
    private int code; // Código de error en caso status = ERROR
    private ServerInfo server_info;
    private String request_id;

    public RequestServiceAnswerMsg() {
    }

    public RequestServiceAnswerMsg(ServerInfo server_info, String request_id) {
        this.status = STATUS_SUCCESS_REQUEST;
        this.server_info = server_info;
        this.request_id = request_id;
    }

    public ServerInfo getServerInfo() {
        return server_info;
    }

    public void setServerInfo(ServerInfo server_info) {
        this.server_info = server_info;
    }

    public String getRequestId() {
        return request_id;
    }

    public void setRequestId(String request_id) {
        this.request_id = request_id;
    }

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