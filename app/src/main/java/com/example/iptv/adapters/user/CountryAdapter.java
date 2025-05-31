package com.example.iptv.adapters.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptv.OOP.Country;
import com.example.iptv.R;

import java.util.List;

public class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.CountryViewHolder> {

    private final Context context;
    private final List<Country> countryList;
    private final OnCountryClickListener listener;

    public interface OnCountryClickListener {
        void onCountryClick(Country country);
    }

    public CountryAdapter(Context context, List<Country> countryList, OnCountryClickListener listener) {
        this.context = context;
        this.countryList = countryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CountryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_country_user, parent, false);
        return new CountryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CountryViewHolder holder, int position) {
        Country country = countryList.get(position);
        holder.textCountryName.setText(country.getName());

        // Load country flag
        Glide.with(context)
                .load(country.getFlagUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.imageCountryFlag);

        holder.itemView.setOnClickListener(v -> listener.onCountryClick(country));
    }

    @Override
    public int getItemCount() {
        return countryList.size();
    }

    public static class CountryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCountryFlag;
        TextView textCountryName;

        public CountryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCountryFlag = itemView.findViewById(R.id.image_country_flag);
            textCountryName = itemView.findViewById(R.id.text_country_name);
        }
    }
}
