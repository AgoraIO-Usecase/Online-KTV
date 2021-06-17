package io.agora.ktv.adapter;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.MusicModel;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemChooseSongListBinding;

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
        holder.mDataBinding.tvName.setText(item.getName());

//        if (RoomManager.Instance(context).isInMusicOrderList(item)) {
//            holder.mDataBinding.btChooseSong.setEnabled(false);
//        } else {
        holder.mDataBinding.btChooseSong.setEnabled(true);
//        }
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemChooseSongListBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
