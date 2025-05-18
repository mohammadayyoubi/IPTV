package com.example.iptv;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.Country;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.CountryDAO;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private Context context;
    private List<Channel> channelList;
    private CountryDAO countryDAO;
    private CategoryDAO categoryDAO;
    private Runnable refreshCallback;

    public ChannelAdapter(Context context, List<Channel> channelList,
                          CountryDAO countryDAO, CategoryDAO categoryDAO,
                          Runnable refreshCallback) {
        this.context = context;
        this.channelList = channelList;
        this.countryDAO = countryDAO;
        this.categoryDAO = categoryDAO;
        this.refreshCallback = refreshCallback;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);

        holder.nameTextView.setText(channel.getName());

        // Get and display category
        Category category = categoryDAO.getById(channel.getCategoryId());
        holder.categoryTextView.setText(category != null ? category.getName() : "Unknown");

        // Load logo
        Glide.with(context)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.channelLogoImageView);

        // Load country flag
        Country country = countryDAO.getById(channel.getCountryId());
        if (country != null && country.getFlagUrl() != null) {
            Glide.with(context)
                    .load(country.getFlagUrl())
                    .placeholder(R.drawable.placeholder_flag)
                    .into(holder.countryFlagImageView);
        }

        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, activity_add_channel.class);
            intent.putExtra(activity_add_channel.EXTRA_CHANNEL_ID, channel.getId());
            context.startActivity(intent);
        });
        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Channel")
                    .setMessage("Are you sure you want to delete this channel?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        ChannelDAO channelDAO = new ChannelDAO(new com.example.iptv.database.DBHelper(context).getWritableDatabase());
                        channelDAO.deleteChanelByID(channel.getId());
                        Toast.makeText(context, "Channel deleted", Toast.LENGTH_SHORT).show();
                        refreshCallback.run();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    public void updateData(List<Channel> newChannels) {
        this.channelList = newChannels;
        notifyDataSetChanged();
    }

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, categoryTextView;
        ImageView channelLogoImageView, countryFlagImageView;
        ImageButton editButton, deleteButton;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.channelNameTextView);
            categoryTextView = itemView.findViewById(R.id.channelCategoryTextView);
            channelLogoImageView = itemView.findViewById(R.id.channelLogoImageView);
            countryFlagImageView = itemView.findViewById(R.id.countryFlagImageView);
            editButton = itemView.findViewById(R.id.editChannelButton);
            deleteButton = itemView.findViewById(R.id.deleteChannelButton);
        }
    }
}
