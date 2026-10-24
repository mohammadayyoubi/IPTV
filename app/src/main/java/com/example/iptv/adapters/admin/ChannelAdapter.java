package com.example.iptv.adapters.admin;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Category;
import com.example.iptv.OOP.Channel;
import com.example.iptv.OOP.Country;
import com.example.iptv.R;
import com.example.iptv.activities.admin.activity_add_channel;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.ChannelDAO;
import com.example.iptv.database.CountryDAO;
import com.example.iptv.ui.InputDialog;

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
        Category category = categoryCache().get(channel.getCategoryId());
        holder.categoryTextView.setText(category != null ? category.getName() : "Unknown");

        // Load logo
        Glide.with(context)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.ic_channel_placeholder)
                .error(R.drawable.ic_channel_placeholder)
                .into(holder.channelLogoImageView);

        // Load country flag
        Country country = countryCache().get(channel.getCountryId());
        Glide.with(context)
                .load(country != null ? country.getFlagUrl() : null)
                .placeholder(R.drawable.ic_flag_placeholder)
                .error(R.drawable.ic_flag_placeholder)
                .into(holder.countryFlagImageView);

        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, activity_add_channel.class);
            intent.putExtra(activity_add_channel.EXTRA_CHANNEL_ID, channel.getId());
            context.startActivity(intent);
        });
        holder.deleteButton.setOnClickListener(v -> {
            InputDialog.confirm(context, "Delete channel",
                    "Delete \"" + channel.getName() + "\" and all of its servers?",
                    context.getString(R.string.delete), true, () -> {
                        ChannelDAO channelDAO = new ChannelDAO(new com.example.iptv.database.DBHelper(context).getWritableDatabase());
                        channelDAO.deleteChanelByID(channel.getId());
                        Toast.makeText(context, "Channel deleted", Toast.LENGTH_SHORT).show();
                        refreshCallback.run();
                    });
        });

    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    // Countries / categories are few: load them once instead of two queries per row
    private java.util.Map<Integer, Country> countries;
    private java.util.Map<Integer, Category> categories;

    private java.util.Map<Integer, Country> countryCache() {
        if (countries == null) {
            countries = new java.util.HashMap<>();
            for (Country c : countryDAO.getAll()) countries.put(c.getId(), c);
        }
        return countries;
    }

    private java.util.Map<Integer, Category> categoryCache() {
        if (categories == null) {
            categories = new java.util.HashMap<>();
            for (Category c : categoryDAO.getAll()) categories.put(c.getId(), c);
        }
        return categories;
    }

    /** Adds the next page at the end of the list. */
    public void appendData(List<Channel> more) {
        if (!(channelList instanceof java.util.ArrayList)) channelList = new java.util.ArrayList<>(channelList);
        int start = channelList.size();
        channelList.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void updateData(List<Channel> newChannels) {
        this.channelList = newChannels;
        notifyDataSetChanged();
    }

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, categoryTextView;
        ImageView channelLogoImageView, countryFlagImageView;
        View editButton, deleteButton;

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
