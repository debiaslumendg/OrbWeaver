package com.orbweaver.scheduler.tables;

/**
 * Representa una request de un servicio por parte de un cliente.
 *
 *
 */
public class RequestServer {
    /**
     * Se supone este ID debe ser único entre  todas las request estre todos los schedulers.
     *
     * TODO: Hacer caso donde un cliente intenta ejecutar un id que existía en un scheduler que se cayó y el id existe
     *      en el nuevo scheduler pero fue generado para otro cliente. Agregar marca única de scheduler?
     *      O utilizar UUID ?
     *
     */
    private String id;

    /**
     * ID del servidor que ejecutará(o ejecuta) el request
     */
    private int idServer;

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

    public enum StatusRequest {
        WAITING_FOR_SERVER_EXEC, // Esperando por el servidor se comunique con el scheduler para decir que la esta ejecutando
        RUNNING,                  // La request está corriendo en el servidor
        DONE                      // El servidor ejecutó la request, el cliente recibió la respuesta y se actualizó la tabla.
    }

    /**
     * Estado del request
     */
    private StatusRequest status = StatusRequest.WAITING_FOR_SERVER_EXEC;

    public int getIdServer() {
        return idServer;
    }

    public void setIdServer(int idServer) {
        this.idServer = idServer;
    }

    public RequestServer(String id, int idServer){

        this.id = id;
        this.idServer = idServer;
    }
}
