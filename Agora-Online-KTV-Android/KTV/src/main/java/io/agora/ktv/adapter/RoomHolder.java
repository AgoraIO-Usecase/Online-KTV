package io.agora.ktv.adapter;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraRoom;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.MyUtil;
import io.agora.ktv.databinding.KtvItemRoomListBinding;

public class RoomHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomListBinding, AgoraRoom> {

    public RoomHolder(@NonNull KtvItemRoomListBinding mBinding) {
        super(mBinding);
        MyUtil.scaleOnTouch(mBinding.getRoot());
    }

    @Override
    public void binding(AgoraRoom data, int selectedIndex) {
        if (data != null) {
            mBinding.bgdItemRoomList.setBackgroundResource(data.getCoverRes());
            mBinding.titleItemRoomList.setText(data.getChannelName());
        }
    }
}