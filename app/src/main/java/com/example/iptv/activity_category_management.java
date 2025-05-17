package com.example.iptv;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
        adapter = new CategoryAdapter(categories, categoryDAO, this::showCategoryDialog, this::loadCategories);
        categoryRecyclerView.setAdapter(adapter);
    }

    private void showCategoryDialog(Category categoryToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(categoryToEdit == null ? "Add Category" : "Edit Category");

        // Input field for category name
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT); // Set input type to plain text
        if (categoryToEdit != null) {
            input.setText(categoryToEdit.getName()); // Pre-fill if editing
        }
        builder.setView(input);

        // "Save" button: insert new or update existing category
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim(); // Get input text
            if (name.isEmpty()) return; // Do nothing if input is empty

            if (categoryToEdit == null) {
                categoryDAO.insert(new Category(name)); // Insert new category
            } else {
                categoryToEdit.setName(name);           // Update category name
                categoryDAO.update(categoryToEdit);     // Save changes to DB
            }
            loadCategories(); // Refresh RecyclerView
        });

        // "Cancel" button to dismiss dialog
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // Show the dialog
        builder.show();
    }
}
