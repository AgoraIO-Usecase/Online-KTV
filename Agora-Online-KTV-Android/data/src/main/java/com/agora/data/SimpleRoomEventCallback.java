package com.agora.data;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;

import org.jetbrains.annotations.NotNull;

@MainThread
public class SimpleRoomEventCallback implements RoomEventCallback {

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
    public void onRoomError(int error) {

    }

    @Override
    public void onMusicAdd(@NonNull MusicModel music) {

    }

    @Override
    public void onMusicDelete(@NonNull MusicModel music) {

    }

    @Override
    public void onMusicChanged(@NonNull MusicModel music) {

    }

    @Override
    public void onMusicEmpty() {

    }

    @Override
    public void onMusicProgress(long total, long cur) {

    }
}
