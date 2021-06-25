package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemMvBinding;

/**
 * MVModel List
 *
 * @author chenhengfei@agora.io
 */
public class MVAdapter extends BaseRecyclerViewAdapter<MVAdapter.MVModel, MVAdapter.ViewHolder> {

    public MVAdapter(@Nullable List<MVModel> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_item_mv;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MVModel item = getItemData(position);
        if (item == null) {
            return;
        }

        holder.mDataBinding.iv.setImageResource(item.resId);
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemMvBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }

    public static class MVModel {
        private int resId;

        public MVModel(int resId) {
            this.resId = resId;
        }

        public int getResId() {
            return resId;
        }

        public void setResId(int resId) {
            this.resId = resId;
        }
    }
}
