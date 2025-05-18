package com.example.iptv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.OOP.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Category category);
        void onDelete(Category category);
    }

    private List<Category> categoryList;
    private final LayoutInflater inflater;
    private final OnItemActionListener listener;

    public CategoryAdapter(List<Category> categoryList, LayoutInflater inflater, OnItemActionListener listener) {
        this.categoryList = categoryList;
        this.inflater = inflater;
        this.listener = listener;
    }

    public void updateData(List<Category> newList) {
        this.categoryList = newList;
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
        Category category = categoryList.get(position);
        holder.nameTextView.setText(category.getName());
        holder.idTextView.setText(String.valueOf(category.getId()));

        holder.editButton.setOnClickListener(v -> listener.onEdit(category));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(category));
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

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
