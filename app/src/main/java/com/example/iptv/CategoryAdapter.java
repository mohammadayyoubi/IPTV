package com.example.iptv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.OOP.Category;
import com.example.iptv.database.CategoryDAO;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface CategoryActionListener {
        void onEdit(Category category);
    }

    private List<Category> categoryList;
    private CategoryDAO categoryDAO;
    private CategoryActionListener editListener;
    private Runnable refreshCallback;

    public CategoryAdapter(List<Category> categoryList, CategoryDAO categoryDAO, CategoryActionListener editListener, Runnable refreshCallback) {
        this.categoryList = categoryList;
        this.categoryDAO = categoryDAO;
        this.editListener = editListener;
        this.refreshCallback = refreshCallback;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.nameTextView.setText(category.getName());
        holder.idTextView.setText(String.valueOf(category.getId()));

        holder.editButton.setOnClickListener(v -> editListener.onEdit(category));

        holder.deleteButton.setOnClickListener(v -> {
            categoryDAO.delete(category.getId());
            refreshCallback.run();
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, idTextView;
        Button editButton, deleteButton;

        public CategoryViewHolder(View itemView) {
            super(itemView);
            idTextView = itemView.findViewById(R.id.categoryIdTextView);
            nameTextView = itemView.findViewById(R.id.categoryNameTextView);
            editButton = itemView.findViewById(R.id.editCategoryButton);
            deleteButton = itemView.findViewById(R.id.deleteCategoryButton);
        }
    }
}
