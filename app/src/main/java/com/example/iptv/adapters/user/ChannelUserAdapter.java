package com.example.iptv.adapters.user;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Channel;
import com.example.iptv.R;
import com.example.iptv.activities.user.ChannelDetailActivity;

import java.util.List;

public class ChannelUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_CHANNEL = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private final Context context;
    private List<Channel> channelList;
    private boolean isLoading = false;

    public ChannelUserAdapter(Context context, List<Channel> channelList) {
        this.context = context;
        this.channelList = channelList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_channel_user, parent, false);
            return new ChannelViewHolder(view);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_CHANNEL) {
            Channel channel = channelList.get(position);
            ChannelViewHolder channelHolder = (ChannelViewHolder) holder;
            channelHolder.textChannelName.setText(channel.getName());

            Glide.with(context)
                    .load(channel.getLogoUrl())
                    .placeholder(R.drawable.placeholder)
                    .into(channelHolder.imageChannelLogo);

            channelHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChannelDetailActivity.class);
                intent.putExtra("channel", channel);
                context.startActivity(intent);
            });
        }
        // LoadingViewHolder doesn't need any binding
    }

    @Override
    public int getItemCount() {
        return channelList.size() + (isLoading ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= channelList.size() && isLoading) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_CHANNEL;
    }

    public void updateList(List<Channel> newList) {
        this.channelList = newList;
        notifyDataSetChanged();
    }

    public void addChannels(List<Channel> newChannels) {
        int startPosition = channelList.size();
        channelList.addAll(newChannels);
        notifyItemRangeInserted(startPosition, newChannels.size());
    }

    public void setLoading(boolean loading) {
        if (isLoading != loading) {
            isLoading = loading;
            if (loading) {
                notifyItemInserted(channelList.size());
            } else {
                notifyItemRemoved(channelList.size());
            }
        }
    }

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView imageChannelLogo;
        TextView textChannelName;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            imageChannelLogo = itemView.findViewById(R.id.image_channel_logo);
            textChannelName = itemView.findViewById(R.id.text_channel_name);
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
