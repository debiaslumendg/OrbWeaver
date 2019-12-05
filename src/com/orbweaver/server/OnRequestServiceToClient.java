package com.orbweaver.server;

import com.orbweaver.commons.ServiceInfo;

import java.util.ArrayList;

public interface OnRequestServiceToClient {
    public ServiceInterfaz getService(String serviceName);

    ArrayList<ServiceInfo> getServicesList();
}
