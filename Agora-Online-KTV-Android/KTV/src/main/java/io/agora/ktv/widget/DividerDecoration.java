package io.agora.ktv.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.baselibrary.util.KTVUtil;

/**
 * 分割线
 *
 * @author chenhengfei@agora.io
 */
public class DividerDecoration extends RecyclerView.ItemDecoration {

    private final int gapHorizontal;
    private final int gapVertical;
    private final int spanCount;

    public DividerDecoration(int spanCount) {
        gapHorizontal = (int) KTVUtil.dp2px(16);
        gapVertical = gapHorizontal;
        this.spanCount = spanCount;
    }

    public DividerDecoration(int spanCount, int gapHorizontal, int gapHeight) {
        this.gapHorizontal = (int) KTVUtil.dp2px(gapHorizontal);
        this.gapVertical = (int) KTVUtil.dp2px(gapHeight);
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int index = parent.getChildAdapterPosition(view);

        if(spanCount == 1){
            outRect.left = gapHorizontal;
            outRect.right = gapHorizontal;
        }else {

            if (index % spanCount == 0) {
                outRect.left = gapHorizontal;
                outRect.right = gapHorizontal / 2;
            } else if (index % spanCount == spanCount - 1) {
                outRect.left = gapHorizontal / 2;
                outRect.right = gapHorizontal;
            } else {
                outRect.left = gapHorizontal / 2;
                outRect.right = gapHorizontal / 2;
            }
        }
        outRect.top = gapVertical;
    }
}
