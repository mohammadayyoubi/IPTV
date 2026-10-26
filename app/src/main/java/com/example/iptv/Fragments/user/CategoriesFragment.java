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
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.R;
import com.example.iptv.OOP.Category;
import com.example.iptv.activities.user.activity_user_filtered_channels;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.adapters.user.CategoryAdapter;
import com.example.iptv.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<Category> categoryList;
    private DBHelper dbHelper;
    private EditText searchEditText;
    private TextView countText;
    private View emptyState;

    public CategoriesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        recyclerView = view.findViewById(R.id.recycler_categories);
        searchEditText = view.findViewById(R.id.CategoriesUserSearchEditText);
        countText = view.findViewById(R.id.countText);
        emptyState = view.findViewById(R.id.emptyState);

        UiUtils.setupGrid(recyclerView, R.dimen.tile_min_row);

        dbHelper = DBHelper.getInstance(requireContext());
        CategoryDAO categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
        categoryList = categoryDAO.getAll();

        adapter = new CategoryAdapter(requireContext(), new ArrayList<>(categoryList), category -> {
            Intent intent = new Intent(requireContext(), activity_user_filtered_channels.class);
            intent.putExtra("filterType", "category");
            intent.putExtra("filterId", category.getId());
            intent.putExtra("filterName", category.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

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

        filterCategories("");
        return view;
    }

    private void filterCategories(String query) {
        List<Category> tempList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        for (Category category : categoryList) {
            // Match the English name and the translated one (e.g. "News" / "أخبار")
            String label = UiUtils.categoryLabel(requireContext(), category.getName()).toLowerCase();
            if (category.getName().toLowerCase().contains(lowerQuery) || label.contains(lowerQuery)) {
                tempList.add(category);
            }
        }
        adapter.updateList(tempList);

        countText.setText(UiUtils.quantity(requireContext(), R.plurals.count_categories, tempList.size()));
        if (categoryList.isEmpty()) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_category,
                    R.string.empty_categories_title, R.string.empty_categories_body);
        } else {
            UiUtils.showEmptyState(emptyState, tempList.isEmpty(), R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
        }
    }
}
