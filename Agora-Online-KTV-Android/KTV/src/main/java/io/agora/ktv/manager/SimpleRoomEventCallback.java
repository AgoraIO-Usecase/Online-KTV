package io.agora.ktv.manager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;

import io.agora.ktv.bean.MusicModel;

@MainThread
public class SimpleRoomEventCallback implements RoomEventCallback {

    @Override
    public void onRoomInfoChanged(@NonNull AgoraRoom room) {

    }

    @Override
    public void onRoomClosed(@NonNull AgoraRoom room, boolean fromUser) {

    }

    @Override
    public void onMemberJoin(@NonNull AgoraMember member) {

    }

    @Override
    public void onMemberLeave(@NonNull AgoraMember member) {

    }

    @Override
    public void onRoleChanged(@NonNull AgoraMember member) {

    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {

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
