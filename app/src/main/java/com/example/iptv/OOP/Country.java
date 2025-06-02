package com.example.iptv.OOP;

import java.util.ArrayList;

public class Country {
    private int id;
    private String name;
    private String flagUrl;

    public Country( String name, String flagUrl) {

        this.name = name;
        this.flagUrl = flagUrl;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFlagUrl() { return flagUrl; }
    public void setFlagUrl(String flagUrl) { this.flagUrl = flagUrl; }

    @Override
    public String toString() {
        return name;
    }
}

