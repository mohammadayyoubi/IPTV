package com.example.iptv;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.iptv.OOP.Category;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.DBHelper;

import java.util.List;

public class activity_category_management extends AppCompatActivity {

    private RecyclerView categoryRecyclerView;
    private CategoryAdapter adapter;
    private CategoryDAO categoryDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        DBHelper dbHelper = new DBHelper(this);
        categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button addButton = findViewById(R.id.addCategoryButton);
        addButton.setOnClickListener(v -> showCategoryDialog(null));

        loadCategories();
    }

    private void loadCategories() {
        List<Category> categories = categoryDAO.getAll();
        adapter = new CategoryAdapter(categories);
        categoryRecyclerView.setAdapter(adapter);
    }

    private void showCategoryDialog(Category categoryToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(categoryToEdit == null ? "Add Category" : "Edit Category");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (categoryToEdit != null) {
            input.setText(categoryToEdit.getName());
        }
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;

            if (categoryToEdit == null) {
                categoryDAO.insert(new Category( name));
            } else {
                categoryToEdit.setName(name);
                categoryDAO.update(categoryToEdit);
            }
            loadCategories();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private List<Category> categoryList;

        public CategoryAdapter(List<Category> categoryList) {
            this.categoryList = categoryList;
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category category = categoryList.get(position);
            holder.nameTextView.setText(category.getName());
            holder.idTextView.setText(String.valueOf(category.getId()));

            holder.editButton.setOnClickListener(v -> showCategoryDialog(category));
            holder.deleteButton.setOnClickListener(v -> {
                categoryDAO.delete(category.getId());
                loadCategories();
            });
        }

        @Override
        public int getItemCount() {
            return categoryList.size();
        }

        class CategoryViewHolder extends RecyclerView.ViewHolder {
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
}
