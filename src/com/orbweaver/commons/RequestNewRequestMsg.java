package com.orbweaver.commons;


import com.orbweaver.server.RequestInfo;

public class RequestNewRequestMsg {
    private int code = Constants.CODE_MESSAGE_NEW_REQUEST;
    private RequestInfo request_info;

    public RequestNewRequestMsg(RequestInfo request_info) {
        this.request_info = request_info;
    }

    public RequestInfo getRequestInfo() {
        return request_info;
    }

    public void setRequestInfo(RequestInfo request_info) {
        this.request_info = request_info;
    }
}