package com.orbweaver.server;

/**
 * Representa una request de un servicio por parte de un cliente.
 *
 *
 */
public class RequestInfo {
    /**
     * Se supone este ID debe ser único entre  todas las request estre todos los schedulers.
     *
     */
    private String id;

    /**
     * ID del servidor que ejecutará(o ejecuta) el request
     */
    private int id_server;


    public enum StatusRequest {
        WAITING_FOR_SERVER_EXEC, // Esperando por el servidor se comunique con el scheduler para decir que la esta ejecutando
        RUNNING,                  // La request está corriendo en el servidor
        DONE                      // El servidor ejecutó la request, el cliente recibió la respuesta y se actualizó la tabla.
    }

    /**
     * Estado del request
     */
    private StatusRequest status = StatusRequest.WAITING_FOR_SERVER_EXEC;

    public RequestInfo(String id, int id_server){
        this.id = id;
        this.id_server = id_server;
    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public int getIdServer() {
        return id_server;
    }

    public void setIdServer(int id_server) {
        this.id_server = id_server;
    }


    @Override
    public String toString() {
        return "Request( id= " + getId()   +  ", is_server= " + getIdServer() + " , status  = "  + getStatus() + " ) ";
    }
}
