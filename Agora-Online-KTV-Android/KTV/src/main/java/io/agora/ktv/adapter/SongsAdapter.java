package io.agora.ktv.adapter;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.MusicModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemChooseSongListBinding;
import io.agora.ktv.manager.RoomManager;

/**
 * 歌曲列表
 *
 * @author chenhengfei@agora.io
 */
public class SongsAdapter extends BaseRecyclerViewAdapter<MusicModel, SongsAdapter.ViewHolder> {

    public SongsAdapter(@Nullable List<MusicModel> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_item_choose_song_list;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicModel item = getItemData(position);
        if (item == null) {
            return;
        }

        Context context = holder.itemView.getContext();
        holder.mDataBinding.tvName.setText(item.getName() + "-" + item.getSinger());

        Glide.with(holder.itemView)
                .load(item.getPoster())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                .into(holder.mDataBinding.iv);

            holder.mDataBinding.btChooseSong.setEnabled(true);
            holder.mDataBinding.btChooseSong.setText(R.string.ktv_room_choose_song);
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemChooseSongListBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
