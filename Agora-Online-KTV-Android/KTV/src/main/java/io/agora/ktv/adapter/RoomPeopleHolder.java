package io.agora.ktv.adapter;

import android.content.Context;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvItemRoomSpeakerBinding;
import io.agora.ktv.manager.RoomManager;
import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;

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


        // Show alert text
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

        showAvatarOrCameraView(member);
    }

    private void showAvatarOrCameraView(AgoraMember member) {
        Context mContext = itemView.getContext();
        User mUser = member.getUser();
        RtcEngine engine = RoomManager.Instance(mContext).getRtcEngine();
        if (mUser != null) {
            if (member.getIsVideoMuted() == 1) { // 未开启摄像头 《==》 移除存在的SurfaceView，显示头像
                mBinding.avatarItemRoomSpeaker.setVisibility(View.VISIBLE);
                if (mBinding.getRoot().getChildAt(0) instanceof CardView) {
                    mBinding.getRoot().removeViewAt(0);
                }
                Glide.with(itemView)
                        .load(mUser.getAvatarRes())
                        .circleCrop()
                        .into(mBinding.avatarItemRoomSpeaker);
            } else { // 开启了摄像头
                mBinding.avatarItemRoomSpeaker.setVisibility(View.INVISIBLE);
                if (mBinding.getRoot().getChildAt(0) instanceof CardView) { // SurfaceView 已存在 《==》 No-OP
                    // NO-OP
                } else {
                    AgoraMember mine = RoomManager.Instance(mContext).getMine();
                    if (mine != null) { //
                        SurfaceView surfaceView = loadRenderView(mContext);
                        if (member.getId().equals(mine.getId())) { // 是本人
                            engine.startPreview();
                            engine.setupLocalVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0));
                        }else {
                            int id = Integer.decode("0x" + member.getId().substring(18, 24));
                            RoomManager.Instance(mContext).getRtcEngine().setupRemoteVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, id));
                        }
                    }
                }
            }
        }
    }

    @NonNull
    private SurfaceView loadRenderView(@NonNull Context mContext) {
        MaterialCardView cardView = new MaterialCardView(mContext, null, R.attr.materialCardViewStyle);
        cardView.setCardElevation(0);
        cardView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> cardView.setRadius((right - left) / 2f));

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 0);
        lp.dimensionRatio = "1:1";
        cardView.setLayoutParams(lp);

        SurfaceView surfaceView = new SurfaceView(mContext);
        surfaceView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        cardView.addView(surfaceView);

        mBinding.getRoot().addView(cardView, 0);

        return surfaceView;
    }

}