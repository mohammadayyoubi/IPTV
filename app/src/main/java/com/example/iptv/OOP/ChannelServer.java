package com.example.iptv.OOP;

import java.io.Serializable;

public class ChannelServer  implements Serializable {
    private int id;
    private int channelId;
    private String serverName;
    private String streamUrl;

    public ChannelServer( int channelId, String serverName, String streamUrl) {

        this.channelId = channelId;
        this.serverName = serverName;
        this.streamUrl = streamUrl;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
}
