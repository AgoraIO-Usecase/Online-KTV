package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemMvBinding;

/**
 * MVModel List
 *
 * @author chenhengfei@agora.io
 */
public class MVAdapter extends BaseRecyclerViewAdapter<Integer, MVAdapter.ViewHolder> {

    private int selectIndex = -1;

    public MVAdapter(@Nullable List<Integer> datas, @Nullable Object listener) {
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
        Integer item = getItemData(position);
        if (item == null) {
            return;
        }

        if (selectIndex == position) {
            holder.mDataBinding.llRoot.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.ktv_mv_selected));
            holder.mDataBinding.ivSelected.setVisibility(View.VISIBLE);
        } else {
            holder.mDataBinding.llRoot.setBackground(null);
            holder.mDataBinding.ivSelected.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView)
                .load(item)
                .into(holder.mDataBinding.iv);
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemMvBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }

    public void setSelectIndex(int selectIndex) {
        this.selectIndex = selectIndex;
        notifyDataSetChanged();
    }

}
