package com.example.iptv.OOP;

import java.util.List;

public class Channel {
    private int id;
    private String name;
    private String logoUrl;
    private int countryId;
    private int categoryId;
    private List<ChannelServer> servers;
    private boolean isFavorite; // Flag to indicate if this channel is marked as a favorite

    public Channel(int id, String name, String logoUrl, int countryId, int categoryId, List<ChannelServer> servers) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.countryId = countryId;
        this.categoryId = categoryId;
        this.servers = servers;
        this.isFavorite = false; // Default to false, will be updated based on favorites DB
    }

    // Getters and Setters
    public int getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public int getCountryId() { return countryId; }
    public void setCountryId(int countryId) { this.countryId = countryId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public List<ChannelServer> getServers() { return servers; }
    public void setServers(List<ChannelServer> servers) { this.servers = servers; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}
