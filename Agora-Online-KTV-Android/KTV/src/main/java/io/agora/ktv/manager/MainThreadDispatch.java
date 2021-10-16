package io.agora.ktv.manager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.agora.ktv.bean.MemberMusicModel;

/**
 * 主要将房间内事件切换到主线程，然后丢给界面。
 */
public class MainThreadDispatch implements RoomEventCallback {
    private final Logger.Builder mLogger = XLog.tag("MainThreadDispatch");

    private static final int ON_MEMBER_JOIN = 1;
    private static final int ON_MEMBER_LEAVE = ON_MEMBER_JOIN + 1;
    private static final int ON_ROLE_CHANGED = ON_MEMBER_LEAVE + 1;
    private static final int ON_AUDIO_CHANGED = ON_ROLE_CHANGED + 1;
    private static final int ON_ROOM_ERROR = ON_AUDIO_CHANGED + 1;
    private static final int ON_ROOM_CLOSED = ON_ROOM_ERROR + 1;
    private static final int ON_MUSIC_ADD = ON_ROOM_CLOSED + 1;
    private static final int ON_MUSIC_DELETE = ON_MUSIC_ADD + 1;
    private static final int ON_MUSIC_CHANGED = ON_MUSIC_DELETE + 1;
    private static final int ON_MUSIC_COUNTDOWN = ON_MUSIC_CHANGED + 1;
    private static final int ON_MUSIC_EMPTY = ON_MUSIC_COUNTDOWN + 1;
    private static final int ON_MUSIC_PROGRESS = ON_MUSIC_EMPTY + 1;
    private static final int ON_ROOM_INFO_CHANGED = ON_MUSIC_PROGRESS + 1;
    private static final int ON_MEMBER_APPLY_JOIN_CHORUS = ON_ROOM_INFO_CHANGED + 1;
    private static final int ON_MEMBER_JOIN_CHORUS = ON_MEMBER_APPLY_JOIN_CHORUS + 1;
    private static final int ON_MEMBER_CHORUS_READY = ON_MEMBER_JOIN_CHORUS + 1;

    private final List<RoomEventCallback> eventCallbacks = new CopyOnWriteArrayList<>();

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.eventCallbacks.add(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.eventCallbacks.remove(callback);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        switch (msg.what) {
            case ON_MEMBER_JOIN:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMemberJoin((AgoraMember) msg.obj);
                }
                break;
            case ON_MEMBER_LEAVE:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMemberLeave((AgoraMember) msg.obj);
                }
                break;
            case ON_ROLE_CHANGED:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoleChanged((AgoraMember) msg.obj);
                }
                break;
            case ON_AUDIO_CHANGED:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onAudioStatusChanged((AgoraMember) msg.obj);
                }
                break;
            case ON_ROOM_ERROR: {
                Bundle bundle = msg.getData();
                int error = bundle.getInt("error");
                String msgError = bundle.getString("msg");

                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoomError(error, msgError);
                }
                break;
            }
            case ON_ROOM_CLOSED: {
                Bundle bundle = msg.getData();
                AgoraRoom room = bundle.getParcelable("room");
                boolean fromUser = bundle.getBoolean("fromUser");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoomClosed(room, fromUser);
                }
                break;
            }
            case ON_MUSIC_ADD: {
                MemberMusicModel data = (MemberMusicModel) msg.obj;
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicAdd(data);
                }
                break;
            }
            case ON_MUSIC_DELETE: {
                MemberMusicModel data = (MemberMusicModel) msg.obj;
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicDelete(data);
                }
                break;
            }
            case ON_MUSIC_EMPTY:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicEmpty();
                }
                break;
            case ON_MUSIC_CHANGED: {
                MemberMusicModel data = (MemberMusicModel) msg.obj;
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicChanged(data);
                }
                break;
            }
            case ON_MUSIC_PROGRESS: {
                Bundle bundle = msg.getData();
                long total = bundle.getLong("total");
                long cur = bundle.getLong("cur");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicProgress(total, cur);
                }
                break;
            }
            case ON_MUSIC_COUNTDOWN:{
                Bundle bundle = msg.getData();
                int uid = bundle.getInt("uid");
                int time = bundle.getInt("time");
                String musicId = bundle.getString("musicId");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onReceivedCountdown(uid, time, musicId);
                }
                break;
            }
            case ON_ROOM_INFO_CHANGED:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoomInfoChanged((AgoraRoom) msg.obj);
                }
                break;
            case ON_MEMBER_APPLY_JOIN_CHORUS:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMemberApplyJoinChorus((MemberMusicModel) msg.obj);
                }
                break;
            case ON_MEMBER_JOIN_CHORUS:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMemberJoinedChorus((MemberMusicModel) msg.obj);
                }
                break;
            case ON_MEMBER_CHORUS_READY:
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMemberChorusReady((MemberMusicModel) msg.obj);
                }
                break;
        }
        return false;
    });

    @Override
    public void onRoomInfoChanged(@NonNull AgoraRoom room) {
    }

    @Override
    public void onRoomClosed(@NonNull AgoraRoom room, boolean fromUser) {
    }

    @Override
    public void onMemberJoin(@NonNull AgoraMember member) {
        mLogger.d("onMemberJoin() called with: member = [%s]", member);
        mHandler.obtainMessage(ON_MEMBER_JOIN, member).sendToTarget();
    }

    @Override
    public void onMemberLeave(@NonNull AgoraMember member) {
        mLogger.d("onMemberLeave() called with: member = [%s]", member);
        mHandler.obtainMessage(ON_MEMBER_LEAVE, member).sendToTarget();
    }

    @Override
    public void onRoleChanged(@NonNull AgoraMember member) {
        mLogger.d("onRoleChanged() called with: member = [%s]", member);
        mHandler.obtainMessage(ON_ROLE_CHANGED, member).sendToTarget();
    }

    @Override
    public void onAudioStatusChanged(@NonNull AgoraMember member) {
        mLogger.d("onAudioStatusChanged() called with: member = [%s]", member);
        mHandler.obtainMessage(ON_AUDIO_CHANGED, member).sendToTarget();
    }

    @Override
    public void onRoomError(int error, String msg) {
        mLogger.d("onRoomError() called with: error = [%s], msg = [%s]", error, msg);
        Bundle bundle = new Bundle();
        bundle.putInt("error", error);
        bundle.putString("msg", msg);

        Message message = mHandler.obtainMessage(ON_ROOM_ERROR);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMusicAdd(@NonNull MemberMusicModel music) {
        mLogger.d("onMusicAdd() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MUSIC_ADD, music).sendToTarget();
    }

    @Override
    public void onMusicDelete(@NonNull MemberMusicModel music) {
        mLogger.d("onMusicDelete() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MUSIC_DELETE, music).sendToTarget();
    }

    @Override
    public void onMusicChanged(@NonNull MemberMusicModel music) {
        mLogger.d("onMusicChanged() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MUSIC_CHANGED, music).sendToTarget();
    }

    @Override
    public void onMusicEmpty() {
        mLogger.d("onMusicEmpty() called");
        mHandler.obtainMessage(ON_MUSIC_EMPTY).sendToTarget();
    }

    @Override
    public void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {
        mLogger.d("onMemberApplyJoinChorus() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MEMBER_APPLY_JOIN_CHORUS, music).sendToTarget();
    }

    @Override
    public void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
        mLogger.d("onMemberJoinChorus() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MEMBER_JOIN_CHORUS, music).sendToTarget();
    }

    @Override
    public void onMemberChorusReady(@NonNull MemberMusicModel music) {
        mLogger.d("onMemberChorusReady() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MEMBER_CHORUS_READY, music).sendToTarget();
    }

    @Override
    public void onMusicProgress(long total, long cur) {
        mLogger.d("onMusicProgress() called with: total = [%s], cur = [%s]", total, cur);
        Bundle bundle = new Bundle();
        bundle.putLong("total", total);
        bundle.putLong("cur", cur);

        Message message = mHandler.obtainMessage(ON_MUSIC_PROGRESS);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onReceivedCountdown(int uid, int time, String musicId) {
        Bundle bundle = new Bundle();
        bundle.putInt("uid", uid);
        bundle.putInt("time", time);
        bundle.putString("musicId", musicId);

        Message message = mHandler.obtainMessage(ON_MUSIC_COUNTDOWN);
        message.setData(bundle);
        message.sendToTarget();
    }

    public void destroy() {
        eventCallbacks.clear();
    }
}
