package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.AgoraMember;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemRoomSpeakerBinding;

/**
 * 房间说话者列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomSpeakerAdapter extends BaseRecyclerViewAdapter<AgoraMember, RoomSpeakerAdapter.ViewHolder> {

    public RoomSpeakerAdapter(@Nullable List<AgoraMember> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getItemCount() {
        return 8;
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_item_room_speaker;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mDataBinding.tvName.setText(String.valueOf(position + 1));
        holder.mDataBinding.ivHead.setImageResource(R.mipmap.ktv_room_speaker_default);

        AgoraMember item = getItemData(position);
        if (item == null) {
            return;
        }

//        holder.mDataBinding.tvName.setText(item.getName());
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomSpeakerBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
