package io.agora.ktv.manager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;

import io.agora.ktv.bean.MemberMusicModel;

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
    public void onAudioStatusChanged(@NonNull AgoraMember member) {

    }

    @Override
    public void onRoomError(int error, String msg) {

    }

    @Override
    public void onMusicAdd(@NonNull MemberMusicModel music) {

    }

    @Override
    public void onMusicDelete(@NonNull MemberMusicModel music) {

    }

    @Override
    public void onMusicChanged(@NonNull MemberMusicModel music) {

    }

    @Override
    public void onMusicEmpty() {

    }


    @Override
    public void onMemberJoinedChorus(@NonNull MemberMusicModel music) {

    }

    @Override
    public void onMemberChorusReady(@NonNull MemberMusicModel music) {

    }

    @Override
    public void onMusicProgress(long total, long cur) {

    }

    /**
     * 合唱模式下，等待加入合唱倒计时
     *
     */

    @Override
    public void onReceivedCountdown(int uid, int time, String musicId){

    }
}
