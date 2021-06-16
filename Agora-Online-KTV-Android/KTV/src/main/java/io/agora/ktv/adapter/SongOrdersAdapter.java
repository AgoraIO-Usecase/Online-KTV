package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import com.agora.data.model.MusicModel;
import io.agora.ktv.databinding.KtvItemChoosedSongListBinding;

/**
 * 已点歌曲列表
 *
 * @author chenhengfei@agora.io
 */
public class SongOrdersAdapter extends BaseRecyclerViewAdapter<MusicModel, SongOrdersAdapter.ViewHolder> {

    public SongOrdersAdapter(@Nullable List<MusicModel> datas, @Nullable Object listener) {
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
        MusicModel item = getItemData(position);
        if (item == null) {
            return;
        }

        holder.mDataBinding.tvNo.setText(String.valueOf(position + 1));
        holder.mDataBinding.tvName.setText(item.getName());

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
