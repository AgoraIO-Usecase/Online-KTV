package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.AgoraRoom;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemRoomListBinding;

/**
 * 房间列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomListAdapter extends BaseRecyclerViewAdapter<AgoraRoom, RoomListAdapter.ViewHolder> {

    public RoomListAdapter(@Nullable List<AgoraRoom> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_item_room_list;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AgoraRoom item = getItemData(position);
        if (item == null) {
            return;
        }

        holder.mDataBinding.tvName.setText(item.getName());
        holder.mDataBinding.view.setBackgroundResource(R.mipmap.portrait01);
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomListBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
