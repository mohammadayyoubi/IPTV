package com.example.iptv;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.*;

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

        adapter = new CategoryAdapter(categoryDAO.getAll(), getLayoutInflater(), new CategoryAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Category category) {
                showCategoryDialog(category);
            }

            @Override
            public void onDelete(Category category) {
                new AlertDialog.Builder(activity_category_management.this)
                        .setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete \"" + category.getName() + "\"?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            categoryDAO.delete(category.getId());
                            loadCategories();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        categoryRecyclerView.setAdapter(adapter);

        // Load categories initially
        loadCategories();

        // Search functionality
        EditText searchEditText = findViewById(R.id.searchCategoryEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add button
        Button addButton = findViewById(R.id.addCategoryButton);
        addButton.setOnClickListener(v -> showCategoryDialog(null));

        // Delete all button
        Button deleteAllButton = findViewById(R.id.deleteAllCategoriesButton);
        deleteAllButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete All Categories")
                    .setMessage("Are you sure you want to delete all categories? That will delete the channels related to categories also!")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        categoryDAO.deleteAllCategories();
                        loadCategories();
                        Toast.makeText(this, "All categories deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadCategories() {
        List<Category> categories = categoryDAO.getAll();
        adapter.updateData(categories);
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
                categoryDAO.insert(new Category(name));
            } else {
                categoryToEdit.setName(name);
                categoryDAO.update(categoryToEdit);
            }
            loadCategories();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
