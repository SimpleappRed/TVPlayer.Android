package com.cy8018.tvplayer.util;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MarginItemDecoration extends RecyclerView.ItemDecoration {

    private final int mMarginTop;
    private final int mMarginBottom;

    public MarginItemDecoration(@NonNull Context context, int top, int bottom) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mMarginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, top, metrics);
        mMarginBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, bottom, metrics);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        final int itemPosition = parent.getChildAdapterPosition(view);
        if (itemPosition == RecyclerView.NO_POSITION) {
            return;
        }
        if (itemPosition == 0) {
            outRect.top = mMarginTop;
        }

        final RecyclerView.Adapter adapter = parent.getAdapter();
        if ((adapter != null) && (itemPosition == adapter.getItemCount() - 1)) {
            outRect.bottom = mMarginBottom;
        }
    }
}
