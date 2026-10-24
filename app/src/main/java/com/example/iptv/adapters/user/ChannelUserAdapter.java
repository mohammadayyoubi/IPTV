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
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Channel;
import com.example.iptv.R;
import com.example.iptv.activities.user.ChannelDetailActivity;
import com.example.iptv.ui.UiUtils;

import java.util.List;

public class ChannelUserAdapter extends RecyclerView.Adapter<ChannelUserAdapter.ChannelViewHolder> {

    private final Context context;
    private List<Channel> channelList;

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
        holder.textChannelName.setText(channel.getName());
        int serverCount = channel.getServerCount();
        Channel.Availability availability = channel.getAvailability();
        boolean watchable = availability == Channel.Availability.WATCHABLE;
        if (availability == Channel.Availability.NO_SERVERS) {
            holder.textChannelMeta.setText(R.string.tile_no_servers);
        } else if (availability == Channel.Availability.NOT_WORKING) {
            holder.textChannelMeta.setText(R.string.tile_not_working);
        } else {
            holder.textChannelMeta.setText(UiUtils.quantity(context, R.plurals.count_servers, serverCount));
        }
        holder.textChannelMeta.setTextColor(ContextCompat.getColor(context,
                watchable ? R.color.text_tertiary : R.color.danger));
        holder.textLiveBadge.setText(watchable ? R.string.live : R.string.offline);
        holder.textLiveBadge.setBackgroundResource(watchable ? R.drawable.bg_badge_live : R.drawable.bg_badge_offline);
        holder.textLiveBadge.setTextColor(ContextCompat.getColor(context,
                watchable ? R.color.white : R.color.text_secondary));
        // Dim tiles that can't be played
        holder.imageChannelLogo.setAlpha(watchable ? 1f : 0.45f);

        Glide.with(context)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.ic_channel_placeholder)
                .error(R.drawable.ic_channel_placeholder)
                .into(holder.imageChannelLogo);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChannelDetailActivity.class);
            // Paged lists don't load servers; the player loads the full channel by id
            intent.putExtra(ChannelDetailActivity.EXTRA_CHANNEL_ID, channel.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    /** Adds the next page at the end of the list. */
    public void appendList(List<Channel> more) {
        if (!(channelList instanceof java.util.ArrayList)) channelList = new java.util.ArrayList<>(channelList);
        int start = channelList.size();
        channelList.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void updateList(List<Channel> newList) {
        this.channelList = newList;
        notifyDataSetChanged();
    }

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView imageChannelLogo;
        TextView textChannelName;
        TextView textChannelMeta;
        TextView textLiveBadge;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            imageChannelLogo = itemView.findViewById(R.id.image_channel_logo);
            textChannelName = itemView.findViewById(R.id.text_channel_name);
            textChannelMeta = itemView.findViewById(R.id.text_channel_meta);
            textLiveBadge = itemView.findViewById(R.id.text_live_badge);
        }
    }
}
