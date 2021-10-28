package io.agora.ktv.adapter;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraRoom;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.databinding.KtvItemRoomListBinding;
import io.agora.ktv.repo.ExampleData;

public class RoomHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomListBinding, AgoraRoom> {

    public RoomHolder(@NonNull KtvItemRoomListBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(AgoraRoom data, int selectedIndex) {
        if (data != null) {
            mBinding.bgdItemRoomList.setBackgroundResource(ExampleData.getCoverRes(data.getCover()));
            mBinding.titleItemRoomList.setText(data.getId());
        }
    }
}