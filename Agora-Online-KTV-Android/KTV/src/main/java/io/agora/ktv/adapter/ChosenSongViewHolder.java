package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;

import com.agora.data.model.MusicModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvItemChoosedSongListBinding;

/**
 * The holder of Item ChooseSong
 */
public class ChosenSongViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemChoosedSongListBinding, MemberMusicModel> {
    public ChosenSongViewHolder(@NonNull KtvItemChoosedSongListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(MemberMusicModel item, int selectedIndex) {
        if (item != null) {
            mBinding.tvNo.setText(String.valueOf(getAdapterPosition() + 1));
            mBinding.tvName.setText(itemView.getContext().getString(R.string.song_and_singer, item.getName(), item.getSinger()));

            Glide.with(itemView)
                    .load(item.getPoster())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                    .into(mBinding.iv);

            if (getAdapterPosition() == 0) {
                mBinding.tvSing.setVisibility(View.VISIBLE);
            } else {
                mBinding.tvSing.setVisibility(View.GONE);
            }

        }
    }
}