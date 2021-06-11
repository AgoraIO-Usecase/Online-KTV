package com.agora.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.Action;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.Member;
import com.agora.data.model.Room;

import org.jetbrains.annotations.NotNull;

@MainThread
public class SimpleRoomEventCallback2 implements RoomEventCallback2 {

    @Override
    public void onRoomClosed(@NonNull @NotNull AgoraRoom room, boolean fromUser) {

    }

    @Override
    public void onMemberJoin(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onMemberLeave(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onRoleChanged(boolean isMine, @NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onSDKVideoStatusChanged(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onReceivedRequest(@NonNull @NotNull AgoraMember member, @NonNull @NotNull Action.ACTION action) {

    }

    @Override
    public void onRequestAgreed(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onRequestRefuse(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onReceivedInvite(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onInviteAgree(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onInviteRefuse(@NonNull @NotNull AgoraMember member) {

    }

    @Override
    public void onEnterMinStatus() {

    }

    @Override
    public void onRoomError(int error) {

    }

    @Override
    public void onRoomMessageReceived(@NonNull @NotNull AgoraMember member, @NonNull @NotNull String message) {

    }
}
