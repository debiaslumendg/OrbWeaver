package com.orbweaver.server;

public class WordCountService implements ServiceInterfaz{

    private String name = "wordcount";

    public static long countWords(String s){
        return 100;
    }

    @Override
    public String getName() {
        return name;
    }
}
