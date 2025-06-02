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

public class ChannelUserAdapter extends RecyclerView.Adapter<ChannelUserAdapter.ChannelViewHolder> {

    private final Context context;
    private List<Channel> channelList; // Removed 'final' to allow updates

    public ChannelUserAdapter(Context context, List<Channel> channelList) {
        this.context = context;
        this.channelList = channelList;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel_user, parent, false);
        return new ChannelViewHolder(view);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);

        // Set channel name
        holder.textChannelName.setText(channel.getName());

        // Load channel logo
        Glide.with(context)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.imageChannelLogo);

        // Click handling: Open ChannelDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChannelDetailActivity.class);
            intent.putExtra("channel", channel);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    // New method to update the channel list dynamically
    public void updateList(List<Channel> newList) {
        this.channelList = newList;
        notifyDataSetChanged();
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
}
