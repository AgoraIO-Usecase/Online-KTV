package com.agora.data.manager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.agora.data.RoomEventCallback;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 主要将房间内事件切换到主线程，然后丢给界面。
 */
public class MainThreadDispatch implements RoomEventCallback {

    private static final int ON_MEMBER_JOIN = 1;
    private static final int ON_MEMBER_LEAVE = ON_MEMBER_JOIN + 1;
    private static final int ON_ROLE_CHANGED = ON_MEMBER_LEAVE + 1;
    private static final int ON_AUDIO_CHANGED = ON_ROLE_CHANGED + 1;
    private static final int ON_ROOM_ERROR = ON_AUDIO_CHANGED + 1;
    private static final int ON_ROOM_CLOSED = ON_ROOM_ERROR + 1;
    private static final int ON_MUSIC_ADD = ON_ROOM_CLOSED + 1;
    private static final int ON_MUSIC_DELETE = ON_MUSIC_ADD + 1;
    private static final int ON_MUSIC_CHANGED = ON_MUSIC_DELETE + 1;
    private static final int ON_MUSIC_EMPTY = ON_MUSIC_CHANGED + 1;
    private static final int ON_MUSIC_PROGRESS = ON_MUSIC_EMPTY + 1;

    private final List<RoomEventCallback> enevtCallbacks = new ArrayList<>();

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.enevtCallbacks.add(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.enevtCallbacks.remove(callback);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == ON_MEMBER_JOIN) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onMemberJoin((AgoraMember) msg.obj);
                }
            } else if (msg.what == ON_MEMBER_LEAVE) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onMemberLeave((AgoraMember) msg.obj);
                }
            } else if (msg.what == ON_ROLE_CHANGED) {
                Bundle bundle = msg.getData();
                boolean isMine = bundle.getBoolean("isMine");
                AgoraMember member = bundle.getParcelable("member");

                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRoleChanged(isMine, member);
                }
            } else if (msg.what == ON_AUDIO_CHANGED) {
                Bundle bundle = msg.getData();
                boolean isMine = bundle.getBoolean("isMine");
                AgoraMember member = bundle.getParcelable("member");

                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onAudioStatusChanged(isMine, member);
                }
            } else if (msg.what == ON_ROOM_ERROR) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRoomError((Integer) msg.obj);
                }
            } else if (msg.what == ON_ROOM_CLOSED) {
                Bundle bundle = msg.getData();
                AgoraRoom room = bundle.getParcelable("room");
                boolean fromUser = bundle.getBoolean("fromUser");
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRoomClosed(room, fromUser);
                }
            } else if (msg.what == ON_MUSIC_ADD) {
                MusicModel data = (MusicModel) msg.obj;
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onMusicAdd(data);
                }
            } else if (msg.what == ON_MUSIC_DELETE) {
                MusicModel data = (MusicModel) msg.obj;
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onMusicDelete(data);
                }
            } else if (msg.what == ON_MUSIC_CHANGED) {
                MusicModel data = (MusicModel) msg.obj;
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onMusicChanged(data);
                }
            } else if (msg.what == ON_MUSIC_PROGRESS) {
                Bundle bundle = msg.getData();
                long total = bundle.getLong("total");
                long cur = bundle.getLong("cur");
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onMusicProgress(total, cur);
                }
            }
            return false;
        }
    });

    @Override
    public void onRoomClosed(@NonNull AgoraRoom room, boolean fromUser) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("room", room);
        bundle.putBoolean("fromUser", fromUser);

        Message message = mHandler.obtainMessage(ON_ROOM_CLOSED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMemberJoin(@NonNull AgoraMember member) {
        mHandler.obtainMessage(ON_MEMBER_JOIN, member).sendToTarget();
    }

    @Override
    public void onMemberLeave(@NonNull AgoraMember member) {
        mHandler.obtainMessage(ON_MEMBER_LEAVE, member).sendToTarget();
    }

    @Override
    public void onRoleChanged(boolean isMine, @NonNull AgoraMember member) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("isMine", isMine);
        bundle.putParcelable("member", member);

        Message message = mHandler.obtainMessage(ON_ROLE_CHANGED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("isMine", isMine);
        bundle.putParcelable("member", member);

        Message message = mHandler.obtainMessage(ON_AUDIO_CHANGED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onRoomError(int error) {
        mHandler.obtainMessage(ON_ROOM_ERROR, error).sendToTarget();
    }


    @Override
    public void onMusicAdd(@NonNull MusicModel music) {
        mHandler.obtainMessage(ON_MUSIC_ADD, music).sendToTarget();
    }

    @Override
    public void onMusicDelete(@NonNull MusicModel music) {
        mHandler.obtainMessage(ON_MUSIC_DELETE, music).sendToTarget();
    }

    @Override
    public void onMusicChanged(@NonNull MusicModel music) {
        mHandler.obtainMessage(ON_MUSIC_CHANGED, music).sendToTarget();
    }

    @Override
    public void onMusicEmpty() {
        mHandler.obtainMessage(ON_MUSIC_EMPTY).sendToTarget();
    }

    @Override
    public void onMusicProgress(long total, long cur) {
        Bundle bundle = new Bundle();
        bundle.putLong("total", total);
        bundle.putLong("cur", cur);

        Message message = mHandler.obtainMessage(ON_MUSIC_PROGRESS);
        message.setData(bundle);
        message.sendToTarget();
    }
}
