package com.example.iptv.ui;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import com.example.iptv.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for "type to search" dropdowns (MaterialAutoCompleteTextView).
 * Matches anywhere in the label, not only the prefix. When the text already equals
 * an item exactly (an option was just picked), it shows every option again so the
 * user can switch to another one.
 */
public class SearchableAdapter<T> extends ArrayAdapter<T> {

    private final List<T> allItems;
    private final List<T> visibleItems;

    public SearchableAdapter(@NonNull Context context, @NonNull List<T> items) {
        this(context, items, new ArrayList<>(items));
    }

    private SearchableAdapter(Context context, List<T> items, List<T> visible) {
        super(context, R.layout.item_dropdown, visible);
        this.allItems = new ArrayList<>(items);
        this.visibleItems = visible;
    }

    /** Returns the item whose label equals {@code text} (ignoring case), or null. */
    public T findByLabel(String text) {
        if (text == null) return null;
        String needle = text.trim();
        for (T item : allItems) {
            if (String.valueOf(item).equalsIgnoreCase(needle)) return item;
        }
        return null;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<T> result = new ArrayList<>();
            String query = constraint == null ? "" : constraint.toString().trim().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || findByLabel(query) != null) {
                result.addAll(allItems);
            } else {
                for (T item : allItems) {
                    if (String.valueOf(item).toLowerCase(Locale.ROOT).contains(query)) {
                        result.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = result;
            results.count = result.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            visibleItems.clear();
            if (results.values != null) visibleItems.addAll((List<T>) results.values);
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return String.valueOf(resultValue);
        }
    };
}
