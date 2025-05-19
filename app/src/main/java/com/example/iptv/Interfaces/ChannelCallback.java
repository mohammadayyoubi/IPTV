package com.example.iptv.Interfaces;

import com.example.iptv.OOP.Channel;

import java.util.List;

public interface ChannelCallback {
    void onChannelsFetched(List<Channel> channels);
}
