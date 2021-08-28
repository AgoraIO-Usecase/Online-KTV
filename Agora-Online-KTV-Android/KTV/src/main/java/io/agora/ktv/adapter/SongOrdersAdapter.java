package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvItemChoosedSongListBinding;

/**
 * 已点歌曲列表
 *
 * @author chenhengfei@agora.io
 */
public class SongOrdersAdapter extends BaseRecyclerViewAdapter<MemberMusicModel, SongOrdersAdapter.ViewHolder> {

    public SongOrdersAdapter(@Nullable List<MemberMusicModel> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_item_choosed_song_list;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemberMusicModel item = getItemData(position);
        if (item == null) {
            return;
        }

        holder.mDataBinding.tvNo.setText(String.valueOf(position + 1));
        holder.mDataBinding.tvName.setText(item.getName() + "-" + item.getSinger());

        Glide.with(holder.itemView)
                .load(item.getPoster())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                .into(holder.mDataBinding.iv);

        if (position == 0) {
            holder.mDataBinding.tvSing.setVisibility(View.VISIBLE);
        } else {
            holder.mDataBinding.tvSing.setVisibility(View.GONE);
        }
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemChoosedSongListBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
