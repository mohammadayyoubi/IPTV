package com.example.iptv.OOP;

import java.io.Serializable;

public class ChannelServer  implements Serializable {
    /** Health of a stream, as last seen by the stream checker or the player. */
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_WORKING = 1;
    public static final int STATUS_BROKEN = 2;

    private int id;
    private int channelId;
    private String serverName;
    private String streamUrl;
    // Some streams only play when these headers are sent (from iptv-org streams.json)
    private String userAgent;
    private String referrer;
    private int status = STATUS_UNKNOWN;
    private long lastChecked;

    public ChannelServer( int channelId, String serverName, String streamUrl) {

        this.channelId = channelId;
        this.serverName = serverName;
        this.streamUrl = streamUrl;
    }

    public ChannelServer(int channelId, String serverName, String streamUrl, String userAgent, String referrer) {
        this(channelId, serverName, streamUrl);
        this.userAgent = userAgent;
        this.referrer = referrer;
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

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getLastChecked() { return lastChecked; }
    public void setLastChecked(long lastChecked) { this.lastChecked = lastChecked; }
}
