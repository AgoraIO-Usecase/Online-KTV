package com.agora.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.Room;

@MainThread
public interface RoomEventCallback {
    /**
     * 房间被关闭
     *
     * @param fromUser true-是我主动关闭，false-被动关闭，比如房主退出房间
     */
    void onRoomClosed(@NonNull Room room, boolean fromUser);

    /**
     * 用户加入房间
     *
     * @param member
     */
    void onMemberJoin(@NonNull Member member);

    /**
     * 用户离开房间，不包括房主
     *
     * @param member
     */
    void onMemberLeave(@NonNull Member member);

    /**
     * 房间角色变化回调，角色变化指：观众和说话人变化
     *
     * @param isMine 是否是我主动触发的回调，true-我主动触发，false-不是我触发，被动触发。
     */
    void onRoleChanged(boolean isMine, @NonNull Member member);

    /**
     * Audio变化回调，这里变化是指：开麦和禁麦
     *
     * @param isMine 是否是我主动触发的回调，true-我主动触发，false-不是我触发，被动触发。
     */
    void onAudioStatusChanged(boolean isMine, @NonNull Member member);

    /**
     * SDK Video变化回调
     */
    void onSDKVideoStatusChanged(@NonNull Member member);

    void onReceivedRequest(@NonNull Member member, @NonNull Action.ACTION action);

    void onRequestAgreed(@NonNull Member member);

    void onRequestRefuse(@NonNull Member member);

    void onReceivedInvite(@NonNull Member member);

    void onInviteAgree(@NonNull Member member);

    void onInviteRefuse(@NonNull Member member);

    void onEnterMinStatus();

    void onRoomError(int error);

    void onRoomMessageReceived(@NonNull Member member, @NonNull String message);
}
