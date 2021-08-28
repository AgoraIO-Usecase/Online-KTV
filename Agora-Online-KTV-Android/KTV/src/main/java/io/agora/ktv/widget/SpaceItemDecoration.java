package io.agora.ktv.widget;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.ktv.R;

/**
 * 分割线
 *
 * @author chenhengfei@agora.io
 */
public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private int spacing = 0;
    private Context context;

    public SpaceItemDecoration(Context context) {
        this.context = context;
        spacing = context.getResources().getDimensionPixelSize(R.dimen.ktv_item_channel_spacing);
    }

    public SpaceItemDecoration(Context context, int spacing) {
        this.context = context;
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(spacing, spacing, spacing, spacing);
    }
}
