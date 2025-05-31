package com.example.iptv.adapters.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Country;
import com.example.iptv.R;

import java.util.ArrayList;
import java.util.List;

public class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.CountryViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Country country);
        void onDelete(Country country);
    }

    private List<Country> fullList;
    private List<Country> filteredList;
    private final LayoutInflater inflater;
    private final OnItemActionListener listener;
    private final Context context;

    public CountryAdapter(Context context, List<Country> countryList, OnItemActionListener listener) {
        this.context = context;
        this.fullList = new ArrayList<>(countryList);
        this.filteredList = countryList;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void updateData(List<Country> countryList) {
        this.fullList = new ArrayList<>(countryList);
        this.filteredList = countryList;
        notifyDataSetChanged();
    }

    public void filter(String text) {
        text = text.toLowerCase();
        filteredList = new ArrayList<>();
        for (Country c : fullList) {
            if (c.getName().toLowerCase().contains(text)) {
                filteredList.add(c);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CountryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_country, parent, false);
        return new CountryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CountryViewHolder holder, int position) {
        Country country = filteredList.get(position);
        holder.nameTextView.setText(country.getName());

        if (country.getFlagUrl() != null && !country.getFlagUrl().isEmpty()) {
            String url = country.getFlagUrl();
            if (url != null && url.endsWith(".svg")) {
                holder.flagImageView.setImageResource(android.R.drawable.ic_menu_gallery); // fallback icon
            } else {
                Glide.with(context).load(url).into(holder.flagImageView);
            }

        } else {
            holder.flagImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.editButton.setOnClickListener(v -> listener.onEdit(country));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(country));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class CountryViewHolder extends RecyclerView.ViewHolder {
        ImageView flagImageView;
        TextView nameTextView;
        ImageButton editButton, deleteButton;

        public CountryViewHolder(@NonNull View itemView) {
            super(itemView);
            flagImageView = itemView.findViewById(R.id.flagImageView);
            nameTextView = itemView.findViewById(R.id.countryNameTextView);
            editButton = itemView.findViewById(R.id.editCountryButton);
            deleteButton = itemView.findViewById(R.id.deleteCountryButton);
        }
    }
}
