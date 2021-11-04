package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.util.KTVUtil;
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
            mBinding.iv.setStrokeWidth(KTVUtil.dp2px(2));
            mBinding.ivSelected.setVisibility(View.VISIBLE);
        } else {
            mBinding.iv.setStrokeWidth(0);
            mBinding.ivSelected.setVisibility(View.GONE);
        }

        Glide.with(mBinding.getRoot())
                .load(data)
                .into(mBinding.iv);
    }
}