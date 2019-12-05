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

    /** Codigo que indica que el mensaje recibido por el Scheduler es para actualiazr el estado de una request */
    public static final int CODE_REQUEST_UPDATE_REQUEST = 3;

    /** Codigo que indica que el mensaje recibido por el Servidor es para crear una nueva request -- en su lista
     * (aunque no es el scheduler, es por si tiene que ser el scheduler más adelante)*/
    public static final int CODE_REQUEST_NEW_REQUEST = 4;

    /**
     * Indica que el mensaje recibido es solo para ver si está vivo
     *
     * TODO: Intentar utilizar los acks de TCP
     */
    public static final int CODE_REQUEST_PING = 5;

    /**
     * Indica que el mensaje recibido es para eliminar un servidor del grupo
     */
    public static final int CODE_REQUEST_DEL_SERVER = 6;


    /** Codigo que indica el estado EXITOSO de la respuesta de un mensaje del Scheduler*/
    public static final int STATUS_SUCCESS_REQUEST = 2;

    /** Codigo que indica el estado ERROR de la respuesta de un mensaje del Scheduler*/
    public static final int STATUS_ERROR_REQUEST = 3;

    /** Código de error que indica que la request a validar por el scheduler tiene un id que no existe*/
    public static final int CODE_ERROR_INVALID_REQUEST_ID_NOT_FOUND = 1;

    /** Codigo de error que indica que la request a validar por el servidor B, no debería estar siendo ejecutada por él*/
    public static final int CODE_ERROR_INVALID_REQUEST_UNAUTHORIZED_EXEC = 2;

    /** Codigo de error que indica que la request a validar por el scheduler es simplemente inválida, sin más detalle*/
    public static final int CODE_ERROR_INVALID_REQUEST = 3;

    /** Codigo de error que indica que la request ya esta siendo ejecutada o ya fue cumplida*/
    public static final int CODE_ERROR_INVALID_REQUEST_DUPLICATED = 4 ;

    /** Codigo de error que indica que el servicio solicitado no tiene un servidor disponible para su ejecucion*/
    public static final int CODE_ERROR_SOLICITED_SERVICE_NOT_SERVER_FOUND = 5 ;

}
