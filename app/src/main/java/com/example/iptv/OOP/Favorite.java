package com.example.iptv.OOP;
public class Favorite {
    private int id;
    private int channelId; // References Channel ID

    public Favorite(int id, int channelId) {
        this.id = id;
        this.channelId = channelId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }
}
