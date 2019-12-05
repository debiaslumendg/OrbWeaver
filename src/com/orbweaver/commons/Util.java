package com.orbweaver.commons;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static com.orbweaver.commons.Constants.*;

public class Util {

    public static String getArgValue(String[] args,String name){

        for(int i = 0; i < args.length;i++){
            if(name.equals(args[i])){
                if(i == args.length - 1 ){
                    return "";
                }else{
                    return args[i + 1];
                }
            }
        }

        return null;
    }

    public static boolean existArgument(String[] args,String name){

        for(int i = 0; i < args.length;i++){
            if(name.equals(args[i])){
                return true;
            }
        }

        return false;
    }

    public static void printOrbWeaverIntro(String program){
        System.out.format("OrbWeaver - %s\n",program);
    }

    public static int getArgInt(String[] argn, String[] args, int defaultValue){
        String value;

        for (String argname: argn) {

            value = getArgValue(args, argname);

            if (value != null) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    System.out.format("Usage: %s <number>\n", argname);
                    System.exit(-1);
                }
            }
        }

        return defaultValue;

    }

    public static boolean existArguments(String[] argn, String[] args){
        for (String argname : argn) {
            if(existArgument(args,argname)){
                return true;
            }
        }
        return false;
    }


    public static String getArgStr(String[] argn, String[] args,String defaultValue){
        String value;

        for (String argname : argn) {

            value = getArgValue(args, argname);
            if (value != null) {
                if (value.equals("")) {
                    System.out.format("Usage: %s <number>\n", argname);
                    System.exit(-1);
                } else {
                    return value;
                }
            }
        }

        return defaultValue;
    }

    public static void printIPHost(){
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            System.out.println("IP Address:- " + ip);
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
    }

    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        int ch;
        StringBuilder sb = new StringBuilder();
        while((ch = inputStream.read()) != -1)
            sb.append((char)ch);
        //reset();
        return sb.toString();
    }

    public static void mostrarErrorPrint(int code) {
        System.out.println("** OrbWeaver -- ERROR**");
        switch (code){
            /** Código de error que indica que la request a validar por el scheduler tiene un id que no existe*/
            case CODE_ERROR_INVALID_REQUEST_ID_NOT_FOUND:
                System.out.println("Error request not found");
                break;

            /** Codigo de error que indica que la request a validar por el servidor B, no debería estar siendo ejecutada por él*/
            case CODE_ERROR_INVALID_REQUEST_UNAUTHORIZED_EXEC:
                System.out.println("Error Server unauthorized");
                break;

            /** Codigo de error que indica que la request a validar por el scheduler es simplemente inválida, sin más detalle*/
            case CODE_ERROR_INVALID_REQUEST:
                System.out.println("Error invalid request");
                break;

            /** Codigo de error que indica que la request ya esta siendo ejecutada o ya fue cumplida*/
            case CODE_ERROR_INVALID_REQUEST_DUPLICATED:
                System.out.println("Error duplicated request");
                break;
            /** El servicio solictado no fue encontrado en ningun server*/
            case CODE_ERROR_SOLICITED_SERVICE_NOT_SERVER_FOUND:
                System.out.println("Not server running with solicited service found");
                System.out.println("Try again later!");
                break;
        }
    }
}

