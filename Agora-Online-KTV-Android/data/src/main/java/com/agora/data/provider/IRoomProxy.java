package com.agora.data.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.Room;

import io.reactivex.Maybe;

public interface IRoomProxy {
    boolean isMembersContainsKey(@NonNull String memberId);

    @Nullable
    Member getMemberById(@NonNull String memberId);

    @Nullable
    Member getMemberByStramId(long streamId);

    Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId);

    boolean isOwner(@NonNull Member member);

    boolean isOwner();

    boolean isOwner(String userId);

    boolean isMine(@NonNull Member member);

    @Nullable
    Member getMine();

    @Nullable
    Room getRoom();

    @Nullable
    Member getOwner();

    void onMemberJoin(@NonNull Member member);

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

    void onReceivedRequest(@NonNull Member member, @NonNull Action.ACTION action);

    void onRequestAgreed(@NonNull Member member, @NonNull Action.ACTION action);

    void onRequestRefused(@NonNull Member member);

    void onReceivedInvite(@NonNull Member member);

    void onInviteAgree(@NonNull Member member);

    void onInviteRefuse(@NonNull Member member);

    void onEnterMinStatus();

    void onRoomError(int error);

    void onRoomMessageReceived(@NonNull Member member, @NonNull String message);
}
