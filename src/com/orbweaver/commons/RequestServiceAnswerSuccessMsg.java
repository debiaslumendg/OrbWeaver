package com.orbweaver.commons;

/**
 * Representa un mensaje de peticion de un servicio por un cliente
 */
public class RequestServiceAnswerSuccessMsg {

    private int status = Constants.STATUS_SUCCESS_REQUEST; // Nombre del servicio a pedir
    private ServerInfo server_info;
    private String request_id;

    public RequestServiceAnswerSuccessMsg(ServerInfo server_info, String request_id) {
        this.status = status;
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
}