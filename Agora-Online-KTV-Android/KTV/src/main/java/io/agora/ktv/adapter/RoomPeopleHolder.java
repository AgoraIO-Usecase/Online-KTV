package io.agora.ktv.adapter;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvItemRoomSpeakerBinding;
import io.agora.ktv.manager.RoomManager;

public class RoomPeopleHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomSpeakerBinding, AgoraMember> {
    public RoomPeopleHolder(@NonNull KtvItemRoomSpeakerBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(AgoraMember member, int selectedIndex) {
        mBinding.titleItemRoomSpeaker.setText(String.valueOf(getAdapterPosition() + 1));
        mBinding.avatarItemRoomSpeaker.setImageResource(R.drawable.ktv_ic_seat);

        if (member == null) return;

        Context mContext = itemView.getContext();
        if (member.getRole() == AgoraMember.Role.Owner) {
            mBinding.titleItemRoomSpeaker.setText(mContext.getString(R.string.ktv_room_owner));
        }

        User mUser = member.getUser();
        if (mUser != null) {
            Glide.with(itemView)
                    .load(mUser.getAvatarRes())
                    .circleCrop()
                    .into(mBinding.avatarItemRoomSpeaker);
        }

        MemberMusicModel mMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMusicModel != null) {
            if (mMusicModel.getType() == MemberMusicModel.SingType.Single) {
                if (RoomManager.Instance(mContext).isSinger(member.getUserId())) {
                    mBinding.titleItemRoomSpeaker.setText(mContext.getString(R.string.ktv_room_sing1));
                }
            } else if (mMusicModel.getType() == MemberMusicModel.SingType.Chorus) {
                if (RoomManager.Instance(mContext).isSinger(member.getUserId())) {
                    mBinding.titleItemRoomSpeaker.setText(mContext.getString(R.string.ktv_room_sing1_1));
                }
            }
        }
    }
}