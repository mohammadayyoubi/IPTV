package com.example.iptv.Fragments.user;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.iptv.R;
import com.example.iptv.OOP.Category;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.adapters.user.CategoryAdapter;

import java.util.List;

public class CategoriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<Category> categoryList;
    private DBHelper dbHelper;

    public CategoriesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        recyclerView = view.findViewById(R.id.recycler_categories);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3)); // 3-column grid

        dbHelper = new DBHelper(requireContext());
        CategoryDAO categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
        categoryList = categoryDAO.getAll(); // Ensure this method exists

        adapter = new CategoryAdapter(requireContext(), categoryList, category -> {
            // Handle category click: e.g., open filtered channel list by category
        });

        recyclerView.setAdapter(adapter);

        return view;
    }
}
