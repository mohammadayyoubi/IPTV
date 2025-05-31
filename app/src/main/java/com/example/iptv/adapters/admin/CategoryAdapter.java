package com.example.iptv.adapters.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.OOP.Category;
import com.example.iptv.R;

import java.util.ArrayList;
import java.util.List;
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> implements Filterable {

    public interface OnItemActionListener {
        void onEdit(Category category);
        void onDelete(Category category);
    }

    private List<Category> fullList;
    private List<Category> filteredList;
    private final LayoutInflater inflater;
    private final OnItemActionListener listener;

    public CategoryAdapter(List<Category> categoryList, LayoutInflater inflater, OnItemActionListener listener) {
        this.fullList = new ArrayList<>(categoryList);
        this.filteredList = categoryList;
        this.inflater = inflater;
        this.listener = listener;
    }

    public void updateData(List<Category> newList) {
        this.fullList = new ArrayList<>(newList);
        this.filteredList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = filteredList.get(position);
        holder.nameTextView.setText(category.getName());
        holder.idTextView.setText(String.valueOf(category.getId()));

        holder.editButton.setOnClickListener(v -> listener.onEdit(category));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(category));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @Override
    public Filter getFilter() {
        return categoryFilter;
    }

    private final Filter categoryFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Category> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(fullList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Category cat : fullList) {
                    if (cat.getName().toLowerCase().contains(filterPattern)) {
                        filtered.add(cat);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filtered;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList.clear();
            filteredList.addAll((List<Category>) results.values);
            notifyDataSetChanged();
        }
    };

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, idTextView;
        ImageButton editButton, deleteButton;

        public CategoryViewHolder(View itemView) {
            super(itemView);
            idTextView = itemView.findViewById(R.id.categoryIdTextView);
            nameTextView = itemView.findViewById(R.id.categoryNameTextView);
            editButton = itemView.findViewById(R.id.editCategoryButton);
            deleteButton = itemView.findViewById(R.id.deleteCategoryButton);
        }
    }
}
