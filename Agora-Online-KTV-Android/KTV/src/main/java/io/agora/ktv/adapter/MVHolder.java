package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemMvBinding;

/**
 * MVModel List
 *
 * @author chenhengfei@agora.io
 */
public class MVHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemMvBinding, Integer>{

    public MVHolder(@NonNull KtvItemMvBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(Integer data, int selectedIndex) {
        if (getAdapterPosition() == selectedIndex) {
            mBinding.llRoot.setBackgroundColor(ContextCompat.getColor(mBinding.getRoot().getContext(), R.color.ktv_mv_selected));
            mBinding.ivSelected.setVisibility(View.VISIBLE);
        } else {
            mBinding.llRoot.setBackground(null);
            mBinding.ivSelected.setVisibility(View.GONE);
        }

        Glide.with(mBinding.getRoot())
                .load(data)
                .into(mBinding.iv);
    }
}