package com.example.iptv.OOP;
public class Country {
    private int id;
    private String name;
    private String flagUrl;

    public Country(int id, String name, String flagUrl) {
        this.id = id;
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
}
