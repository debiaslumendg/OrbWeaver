package com.orbweaver.server;

import com.orbweaver.commons.Constants;
import com.orbweaver.commons.ServerInfo;
import com.orbweaver.commons.ServiceInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import static com.orbweaver.commons.Util.*;

public class WordCountIsPrimeServer implements  OnRequestServiceToClient {

    private ArrayList<ServiceInfo> services = new ArrayList<ServiceInfo>();
    private ArrayList<ServiceInterfaz> servicesExec = new ArrayList<ServiceInterfaz>();

    public WordCountIsPrimeServer() {

        WordCountService wordCountService = new WordCountService();
        IsPrimeService isPrimeService = new IsPrimeService();

        services.add(new ServiceInfo(wordCountService.getName()));
        services.add(new ServiceInfo(isPrimeService.getName()));

        servicesExec.add(wordCountService);
        servicesExec.add(isPrimeService);

    }

    @Override
    public ServiceInterfaz getService(String serviceName) {
        for(ServiceInterfaz service:servicesExec){
            if(service.getName().equals(serviceName)){
                return service;
            }
        }
        return null;
    }

    @Override
    public ArrayList<ServiceInfo> getServicesList() {
        return services;
    }

    /**
     * Usage
     *  $> wcprimeserver [--port|-p <number>] <--hostc|-h <address> | <--as-scheduler | --sh> >  [--portc|-pc <number>]  [--ports|-ps <NUMBER>]
     *
     *      --port  | -p  : Puerto en donde el servidor va a recibir mensajes(Opcional, se usara el default)
     *      --hostc | -c  : Address del coordinador (Scheduler), obligatorio para agregarse al grupo si no se pasa --as-scheduler
     *      --portc | -pc : Puerto del coordinador (opcional, se usara el default)
     *      --ports | -ps : Puerto en donde el scheduler recibirá mensajes (Opcional, se usara el default)
     *      --as-scheduler | --sh
     *
     *      Ejemplo 1: Servidor y Scheduler
     *      wcprimeserver --sh

     *      Ejemplo 2: Servidor
     *      wcprimeserver -h 127.0.0.1
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

        printOrbWeaverIntro("WordCount Server");
        printIPHost();

        if(!isScheduler && StringUtils.isEmpty(coordinatorAddress)){
            String[] addressport = askUserSchedulerAddress(portCoordinator);
            coordinatorAddress = addressport[0];
            portCoordinator = Integer.parseInt(addressport[1]);

        }

        WordCountIsPrimeServer wordCountServer = new WordCountIsPrimeServer();

        Server server = new Server(portServer,coordinatorAddress,portCoordinator,portScheduler,isScheduler);
        server.setOnRequestServiceToClient(wordCountServer);
        server.run();
    }

}
