package com.example.iptv.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Even gaps between grid tiles, whatever the span count. The outer edges get no gap:
 * the RecyclerView's own padding provides the page gutter.
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int gap;

    public GridSpacingItemDecoration(int gap) {
        this.gap = gap;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;

        int spanCount = 1;
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
        }
        int column = position % spanCount;

        // Split the gap so every tile ends up the same width. "start"/"end" follow the
        // layout direction: in Arabic (right-to-left) the first column is on the right.
        int start = column * gap / spanCount;
        int end = gap - (column + 1) * gap / spanCount;
        boolean rtl = parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        outRect.left = rtl ? end : start;
        outRect.right = rtl ? start : end;
        if (position >= spanCount) {
            outRect.top = gap;
        }
    }
}
