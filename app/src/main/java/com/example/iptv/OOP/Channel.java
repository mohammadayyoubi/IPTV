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
    private String sourceId; // iptv-org channel id (e.g. "BBCOne.uk"), null for channels added by hand
    private boolean logoAuto; // logo came from an import, so updates may replace it
    // Filled by paged list queries, which don't load the servers themselves
    private int serverCount;
    private int brokenServerCount;

    /** Whether a viewer can actually watch the channel, derived from its servers' health. */
    public enum Availability {
        /** At least one server works, or hasn't been checked yet */
        WATCHABLE,
        /** Has servers, but every one of them failed its last check */
        NOT_WORKING,
        /** No stream URL at all */
        NO_SERVERS
    }

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

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public boolean isLogoAuto() { return logoAuto; }
    public void setLogoAuto(boolean logoAuto) { this.logoAuto = logoAuto; }

    /** Number of servers, whether or not the server list itself was loaded. */
    public int getServerCount() { return servers != null ? servers.size() : serverCount; }

    public void setServerStats(int serverCount, int brokenServerCount) {
        this.serverCount = serverCount;
        this.brokenServerCount = brokenServerCount;
    }

    public Availability getAvailability() {
        if (servers == null) {
            if (serverCount == 0) return Availability.NO_SERVERS;
            return brokenServerCount >= serverCount ? Availability.NOT_WORKING : Availability.WATCHABLE;
        }
        if (servers.isEmpty()) return Availability.NO_SERVERS;
        for (ChannelServer server : servers) {
            if (server.getStatus() != ChannelServer.STATUS_BROKEN) return Availability.WATCHABLE;
        }
        return Availability.NOT_WORKING;
    }


}
