package com.orbweaver.server;

/**
 * Representa un mensaje con los argumentos para el servicio contador de palabras
 */
public class ServiceWordCountArgMsg {
    private int EOF = 0; // 0 para indicar que este mensaje no es un fin de archivo, 1 para indicar que este es el ultimo mensaje
    private String line = ""; // Linea del archivo

    public ServiceWordCountArgMsg(int EOF, String line){
        this.EOF = EOF;
        this.line = line;
    }
    public boolean isEOF() {
        return EOF == 1;
    }

    public void setEOF(int EOF) {
        this.EOF = EOF;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }
}
