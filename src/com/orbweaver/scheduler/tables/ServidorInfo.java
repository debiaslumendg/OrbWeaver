package com.orbweaver.scheduler.tables;

import java.util.ArrayList;
import java.util.Set;

public class ServidorInfo {

    private int id;
    private String name;
    private String address;
    private int port; // Optional: for debugging purposes , various servers running in same machine.
    private ArrayList<Integer> services = new ArrayList<>(10);

    public ServidorInfo(int id, String name, String address){

    }

}
