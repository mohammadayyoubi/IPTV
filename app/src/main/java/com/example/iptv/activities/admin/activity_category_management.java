package com.example.iptv.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.adapters.admin.CategoryAdapter;
import com.example.iptv.OOP.Category;
import com.example.iptv.R;
import com.example.iptv.database.CategoryDAO;
import com.example.iptv.database.DBHelper;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.InputDialog;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class activity_category_management extends BaseActivity {

    private RecyclerView categoryRecyclerView;
    private CategoryAdapter adapter;
    private CategoryDAO categoryDAO;
    private TextView countText;
    private View emptyState;
    private int totalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);
        UiUtils.applyInsets(findViewById(R.id.root), true, true);

        DBHelper dbHelper = DBHelper.getInstance(this);
        categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());

        countText = findViewById(R.id.countText);
        emptyState = findViewById(R.id.emptyState);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        UiUtils.setupGrid(categoryRecyclerView, R.dimen.tile_min_admin_row);

        adapter = new CategoryAdapter(categoryDAO.getAll(), getLayoutInflater(), new CategoryAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Category category) {
                showCategoryDialog(category);
            }

            @Override
            public void onDelete(Category category) {
                InputDialog.confirm(activity_category_management.this, getString(R.string.adm_delete_category),
                        getString(R.string.adm_delete_category_body, category.getName()),
                        getString(R.string.delete), true, () -> {
                            categoryDAO.delete(category.getId());
                            loadCategories();
                        });
            }
        });

        categoryRecyclerView.setAdapter(adapter);
        UiUtils.onAdapterChanged(adapter, this::updateState);

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

        ExtendedFloatingActionButton addButton = findViewById(R.id.addCategoryButton);
        addButton.setOnClickListener(v -> showCategoryDialog(null));
        UiUtils.shrinkFabOnScroll(categoryRecyclerView, addButton);

        // Adds every iptv-org category that isn't here yet (existing ones are kept)
        com.google.android.material.button.MaterialButton loadAll = findViewById(R.id.loadAllCategoriesButton);
        loadAll.setOnClickListener(v -> {
            if (com.example.iptv.AdminTasks.isRunning()) {
                Toast.makeText(this, R.string.task_running, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!com.example.iptv.NetworkMonitor.hasValidatedNetwork(this)) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setIcon(R.drawable.ic_wifi_off)
                        .setTitle(R.string.offline_title)
                        .setMessage(R.string.offline_body)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            loadAll.setEnabled(false);
            loadAll.setIcon(spinner());
            com.example.iptv.getFromInternet.loadAllCategories(daos(dbHelper), new com.example.iptv.getFromInternet.ImportCallback() {
                @Override public void onProgress(String status, int percent, boolean paused) { }

                @Override
                public void onDone(com.example.iptv.ImportOptions.Result result) {
                    resetLoadAll(loadAll);
                    loadCategories();
                    Toast.makeText(activity_category_management.this, result.newCategories == 0
                            ? getString(R.string.adm_all_categories_here)
                            : getString(R.string.adm_categories_added, result.newCategories), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    resetLoadAll(loadAll);
                    Toast.makeText(activity_category_management.this, getString(R.string.adm_categories_load_failed, message), Toast.LENGTH_LONG).show();
                }
            });
        });

        findViewById(R.id.deleteAllCategoriesButton).setOnClickListener(v ->
                InputDialog.confirm(this, getString(R.string.adm_delete_all_categories),
                        getString(R.string.adm_delete_all_categories_body),
                        getString(R.string.delete_all), true, () -> {
                            categoryDAO.deleteAllCategories();
                            loadCategories();
                            Toast.makeText(this, R.string.adm_all_categories_deleted, Toast.LENGTH_SHORT).show();
                        }));
    }

    private com.example.iptv.getFromInternet.Daos daos(DBHelper dbHelper) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        return new com.example.iptv.getFromInternet.Daos(this,
                new com.example.iptv.database.ChannelDAO(db), new com.example.iptv.database.CountryDAO(db),
                new CategoryDAO(db), new com.example.iptv.database.ChannelServerDAO(db));
    }

    /** Small spinner shown in the button while categories download. */
    private android.graphics.drawable.Drawable spinner() {
        com.google.android.material.progressindicator.CircularProgressIndicatorSpec spec =
                new com.google.android.material.progressindicator.CircularProgressIndicatorSpec(this, null, 0,
                        com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
        spec.indicatorColors = new int[]{getColor(R.color.text_primary)};
        return com.google.android.material.progressindicator.IndeterminateDrawable.createCircularDrawable(this, spec);
    }

    private void resetLoadAll(com.google.android.material.button.MaterialButton button) {
        button.setEnabled(true);
        button.setIconResource(R.drawable.ic_cloud_download);
    }

    private void loadCategories() {
        List<Category> categories = categoryDAO.getAll();
        totalCount = categories.size();
        adapter.updateData(categories);
    }

    private void updateState() {
        int shown = adapter.getItemCount();
        countText.setText(UiUtils.quantity(this, R.plurals.count_categories, totalCount));
        if (totalCount == 0) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_category,
                    R.string.empty_categories_title, R.string.empty_categories_body);
        } else {
            UiUtils.showEmptyState(emptyState, shown == 0, R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
        }
    }

    private void showCategoryDialog(Category categoryToEdit) {
        new InputDialog(this)
                .title(categoryToEdit == null ? getString(R.string.add_category) : getString(R.string.adm_edit_category))
                .field(getString(R.string.category_name), categoryToEdit != null ? categoryToEdit.getName() : null)
                .onSubmit((name, unused) -> {
                    if (name.isEmpty()) return;

                    if (categoryToEdit == null) {
                        categoryDAO.insert(new Category(name));
                    } else {
                        categoryToEdit.setName(name);
                        categoryDAO.update(categoryToEdit);
                    }
                    loadCategories();
                })
                .show();
    }
}
