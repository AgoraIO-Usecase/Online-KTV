package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.MyUtil;
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
            mBinding.iv.setStrokeWidth(1);
            mBinding.ivSelected.setVisibility(View.VISIBLE);
            MyUtil.clearStateListAnimator(itemView);
        } else {
            mBinding.iv.setStrokeWidth(0);
            mBinding.ivSelected.setVisibility(View.GONE);
            MyUtil.scaleOnTouch(itemView);
        }

        Glide.with(mBinding.getRoot())
                .load(data)
                .into(mBinding.iv);
    }
}