package com.agora.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.Room;

@MainThread
public class SimpleRoomEventCallback implements RoomEventCallback {

    @Override
    public void onRoomClosed(@NonNull Room room, boolean fromUser) {

    }

    @Override
    public void onMemberJoin(@NonNull Member member) {

    }

    @Override
    public void onMemberLeave(@NonNull Member member) {

    }

    @Override
    public void onRoleChanged(boolean isMine, @NonNull Member member) {

    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull Member member) {

    }

    @Override
    public void onSDKVideoStatusChanged(@NonNull Member member) {

    }

    @Override
    public void onReceivedRequest(@NonNull Member member, @NonNull Action.ACTION action) {

    }


    @Override
    public void onRequestAgreed(@NonNull Member member) {

    }

    @Override
    public void onRequestRefuse(@NonNull Member member) {

    }

    @Override
    public void onReceivedInvite(@NonNull Member member) {

    }

    @Override
    public void onInviteAgree(@NonNull Member member) {

    }

    @Override
    public void onInviteRefuse(@NonNull Member member) {

    }

    @Override
    public void onEnterMinStatus() {

    }

    @Override
    public void onRoomError(int error) {

    }

    @Override
    public void onRoomMessageReceived(@NonNull Member member, @NonNull String message) {

    }
}
