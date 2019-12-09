package com.orbweaver.server;

import com.google.gson.Gson;
import com.orbweaver.commons.Constants;
import com.orbweaver.commons.ServiceInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.orbweaver.commons.Util.*;

public class SortArrayServer implements  OnRequestServiceToClient {

    private SortArrayService sortArrayService = new SortArrayService();
    private ServiceInfo serviceInfoSortArray = new ServiceInfo("sortArray");
    private ArrayList<ServiceInfo> services = new ArrayList<ServiceInfo>();

    public SortArrayServer() {
        services.add(serviceInfoSortArray);
    }

    @Override
    public ServiceInterfaz getService(String serviceName) {
        if(serviceName.equals("sortArray")) {
            return sortArrayService;
        }
        return null;
    }

    @Override
    public ArrayList<ServiceInfo> getServicesList() {
        return services;
    }

    /**
     * Usage
     *  $> sortarrayserver [--port|-p <number>] <--hostc|-h <address> | <--as-scheduler | --sh> >  [--portc|-pc <number>]  [--ports|-ps <NUMBER>]
     *
     *      --port  | -p  : Puerto en donde el servidor va a recibir mensajes(Opcional, se usara el default)
     *      --hostc | -c  : Address del coordinador (Scheduler), obligatorio para agregarse al grupo si no se pasa --as-scheduler
     *      --portc | -pc : Puerto del coordinador (opcional, se usara el default)
     *      --ports | -ps : Puerto en donde el scheduler recibirá mensajes (Opcional, se usara el default)
     *      --as-scheduler | --sh
     *
     *      Ejemplo 1: Servidor y Scheduler
     *      sortarrayserver --sh

     *      Ejemplo 2: Servidor
     *      sortarrayserver -h 127.0.0.1
     *
     *      Si estan en la misma máquina se pueden utilizar los argumentos de puertos para diferenciarlos.
     *
     * @param args
     */
    public static void main(String[] args) {

        int portServer = Constants.DEFAULT_SERVER_PORT;
        int portScheduler = Constants.DEFAULT_SCHEDULER_PORT;
        int portCoordinator = Constants.DEFAULT_SCHEDULER_PORT;
        boolean isScheduler = false;

        String coordinatorAddress = "";

        if (args.length >= 1) {

            portServer = getArgInt(new String[]{"--port","-p"},args,portServer);

            portScheduler = getArgInt(new String[]{"--ports","-ps"},args,portScheduler);

            coordinatorAddress = getArgStr(new String[]{"--hostc","-c"},args,null);

            portCoordinator = getArgInt(new String[]{"--portc","-pc"},args,portCoordinator);

            isScheduler = existArguments(new String[]{"--as-sh","--sh"},args);
        }

        printOrbWeaverIntro("SortArray Server");
        System.out.println("IP Address:- " + getIPHost());

        if(!isScheduler && StringUtils.isEmpty(coordinatorAddress)){
            String[] addressport = askUserSchedulerAddress(portCoordinator);
            coordinatorAddress = addressport[0];
            portCoordinator = Integer.parseInt(addressport[1]);

        }

        SortArrayServer sortArrayServer = new SortArrayServer();

        Server server = new Server(portServer,coordinatorAddress,portCoordinator,portScheduler,isScheduler);
        server.setOnRequestServiceToClient(sortArrayServer);
        server.run();
    }

}


