package com.orbweaver.commons;


import com.orbweaver.server.RequestInfo;

public class RequestNewRequestMsg {
    private int code = Constants.CODE_REQUEST_NEW_REQUEST;
    private RequestInfo request_info;

    public RequestNewRequestMsg(RequestInfo request_info) {
        this.request_info = request_info;
    }

    public RequestInfo getRequest_info() {
        return request_info;
    }

    public void setRequest_info(RequestInfo request_info) {
        this.request_info = request_info;
    }
}