package com.example.iptv.Fragments.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.R;
import com.example.iptv.OOP.Category;
import com.example.iptv.activities.user.activity_user_filtered_channels;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.adapters.user.CategoryAdapter;

import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<Category> categoryList;
    private List<Category> filteredList;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private TextView pageTitle;

    public CategoriesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_categories);
        searchEditText = view.findViewById(R.id.CategoriesUserSearchEditText);
        pageTitle = view.findViewById(R.id.categoriesUserPageLabel);

        if (pageTitle != null) {
            pageTitle.setText("All Categories List");
        } else {
            System.err.println("categoriesUserPageLabel not found in layout.");
        }

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        // Initialize database helper and DAO
        dbHelper = new DBHelper(requireContext());
        CategoryDAO categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
        categoryList = categoryDAO.getAll(); // Fetch all categories
        filteredList = new ArrayList<>(categoryList);

        // Initialize adapter
        adapter = new CategoryAdapter(requireContext(), categoryList, category -> {
            Intent intent = new Intent(requireContext(), activity_user_filtered_channels.class);
            intent.putExtra("filterType", "category");
            intent.putExtra("filterId", category.getId());
            intent.putExtra("filterName", category.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Add search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        return view;
    }

    private void filterCategories(String query) {
        List<Category> tempList = new ArrayList<>();
        if (query.isEmpty()) {
            tempList.addAll(categoryList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Category category : categoryList) {
                if (category.getName().toLowerCase().contains(lowerQuery)) {
                    tempList.add(category);
                }
            }
        }
        adapter.updateList(tempList);
    }
}
