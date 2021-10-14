package io.agora.ktv.adapter;

import android.view.View;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.agora.data.model.MusicModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.MyUtil;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemChooseSongListBinding;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;

/**
 * The holder of Item ChooseSong
 */
public class ChooseSongViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemChooseSongListBinding, MusicModel> {
    public ChooseSongViewHolder(@NonNull KtvItemChooseSongListBinding mBinding) {
        super(mBinding);
        MyUtil.scaleOnTouch(mBinding.btnItemSongList);
    }

    @Override
    public void binding(MusicModel data, int selectedIndex) {
        if (data != null) {
            mBinding.btnItemSongList.setOnClickListener(v -> RoomChooseSongDialog.finishChooseMusic(itemView.getContext(), data));
            mBinding.titleItemSongList.setText(mBinding.getRoot().getContext().getString(R.string.song_and_singer, data.getName(), data.getSinger()));

            if (URLUtil.isValidUrl(data.getPoster())) {
                mBinding.coverItemSongList.setVisibility(View.VISIBLE);
                Glide.with(itemView)
                        .load(data.getPoster())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                        .into(mBinding.coverItemSongList);
            } else {
                mBinding.coverItemSongList.setVisibility(View.GONE);
            }

            mBinding.btnItemSongList.setEnabled(true);
            mBinding.btnItemSongList.setText(R.string.ktv_room_choose_song);
        }
    }
}