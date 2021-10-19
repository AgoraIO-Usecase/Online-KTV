package io.agora.ktv.adapter;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.User;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.MyUtil;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvItemRoomSpeakerBinding;

public class RoomPeopleHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomSpeakerBinding, AgoraMember> {
    public RoomPeopleHolder(@NonNull KtvItemRoomSpeakerBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(AgoraMember member, int selectedIndex) {
        // title & Icon

        // Default title
        mBinding.titleItemRoomSpeaker.setText(String.valueOf(getAdapterPosition() + 1));

        if (member == null) {
            mBinding.avatarItemRoomSpeaker.setImageResource(R.mipmap.ktv_room_speaker_default);
            MyUtil.scaleOnTouch(itemView);
        }else {
            MyUtil.clearStateListAnimator(itemView);
            // User's special avatar
            User mUser = member.getUser();
            if (mUser != null) {
                mBinding.avatarItemRoomSpeaker.setImageResource(mUser.getAvatarRes());
            } else {
                mBinding.avatarItemRoomSpeaker.setImageResource(R.mipmap.default_head);
            }

        }
    }
}