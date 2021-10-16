package io.agora.ktv.manager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;

import io.agora.ktv.bean.MemberMusicModel;

@MainThread
public interface RoomEventCallback {

    void onRoomInfoChanged(@NonNull AgoraRoom room);

    /**
     * 房间被关闭
     *
     * @param fromUser true-是我主动关闭，false-被动关闭，比如房主退出房间
     */
    void onRoomClosed(@NonNull AgoraRoom room, boolean fromUser);

    /**
     * 用户加入房间
     *
     * @param member
     */
    void onMemberJoin(@NonNull AgoraMember member);

    /**
     * 用户离开房间，不包括房主
     *
     * @param member
     */
    void onMemberLeave(@NonNull AgoraMember member);

    /**
     * 房间角色变化回调，角色变化指：观众和说话人变化
     */
    void onRoleChanged(@NonNull AgoraMember member);

    /**
     * Audio变化回调，这里变化是指：开麦和禁麦
     */
    void onAudioStatusChanged(@NonNull AgoraMember member);

    void onRoomError(int error, String msg);

    void onMusicAdd(@NonNull MemberMusicModel music);

    void onMusicDelete(@NonNull MemberMusicModel music);

    void onMusicChanged(@NonNull MemberMusicModel music);

    void onMusicEmpty();

    /**
     * 合唱模式下，有用户申请加入合唱。
     *
     * @param music
     */
    void onMemberApplyJoinChorus(@NonNull MemberMusicModel music);

    /**
     * 合唱模式下，有用户加入合唱。
     *
     * @param music
     */
    void onMemberJoinedChorus(@NonNull MemberMusicModel music);

    /**
     * 合唱模式下，唱歌人准备就绪。
     *
     * @param music
     */
    void onMemberChorusReady(@NonNull MemberMusicModel music);

    /**
     * 歌曲进度
     *
     * @param total 当前时间，毫秒
     * @param cur   总共时间，毫秒
     */
    void onMusicProgress(long total, long cur);


    /**
     * 合唱模式下，等待加入合唱倒计时
     *
     * @param uid
     * @param time    秒
     * @param musicId
     */
    void onReceivedCountdown(int uid, int time, String musicId);
}
