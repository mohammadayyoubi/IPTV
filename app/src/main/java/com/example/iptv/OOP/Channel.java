package com.example.iptv.OOP;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class Channel implements Serializable {
    private int id;
    private String name;
    private String logoUrl;
    private int countryId;
    private int categoryId;
    private List<ChannelServer> servers;
    private boolean isFavorite; // Flag to indicate if this channel is marked as a favorite

    public Channel( String name, String logoUrl, int countryId, int categoryId, List<ChannelServer> servers) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.countryId = countryId;
        this.categoryId = categoryId;
        this.servers = servers;
        this.isFavorite = false; // Default to false, will be updated based on favorites DB
    }

    protected Channel(Parcel in) {
        id = in.readInt();
        name = in.readString();
        logoUrl = in.readString();
        countryId = in.readInt();
        categoryId = in.readInt();
        isFavorite = in.readByte() != 0;
    }


    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
