package com.orbweaver.commons;

/**
 * Constantes de uso global en el programa
 */
public class Constants {

    /** Puerto default para el Scheduler*/
    public static final int DEFAULT_SCHEDULER_PORT  = 7060;
    /** Puerto default para el Servidor*/
    public static final int DEFAULT_SERVER_PORT     = 7070;

    /** Codigo que indica que el mensaje recibido por el Scheduler es para agregar un servidor
    * Codigo que indica que el mensaje recibido por el Servidor es para un servidor agregado por el Scheduler*/
    public static final int CODE_REQUEST_ADD_SERVER  = 1;
    /** Codigo que indica que el mensaje recibido por el Scheduler es para pedir el servidor para ejecutar un servicio
    * Codigo que indica que el mensaje recibido por el Servidor es para ejecutar un servicio*/
    public static final int CODE_REQUEST_EXEC_SERVICE  = 2;

    /** Codigo que indica el estado EXITOSO de la respuesta de un mensaje del Scheduler*/
    public static final int STATUS_SUCCESS_REQUEST = 2;

    /** Codigo que indica el estado ERROR de la respuesta de un mensaje del Scheduler*/
    public static final int STATUS_ERROR_REQUEST = 3;


}
