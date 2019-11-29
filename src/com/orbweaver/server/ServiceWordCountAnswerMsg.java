package com.orbweaver.server;

/**
 * Representa el mensaje final del servicio wordcount con el numero de palabras contadas
 */
public class ServiceWordCountAnswerMsg {
    private int wordcount = 0; // Numero de palabras leidas

    ServiceWordCountAnswerMsg(int wordcount){
        this.wordcount = wordcount;
    }

    public int getWordcount() {
        return wordcount;
    }

    public void setWordcount(int wordcount) {
        this.wordcount = wordcount;
    }
}
