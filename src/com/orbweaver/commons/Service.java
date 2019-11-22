package com.orbweaver.commons;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class Service {

    private Long id;
    private String name;
    private String description;
    private List<String> params_types = new ArrayList<>();
    private List<String> param_names = new ArrayList<>();
    private String code;

    public Service() {

    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getParams_types() {
        return params_types;
    }

    public void setParams_types(List<String> params_types) {
        this.params_types = params_types;
    }

    public List<String> getParam_names() {
        return param_names;
    }

    public void setParam_names(List<String> param_names) {
        this.param_names = param_names;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    

}
