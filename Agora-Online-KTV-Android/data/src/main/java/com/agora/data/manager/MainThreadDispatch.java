package com.agora.data.manager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.agora.data.RoomEventCallback;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.Room;

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
    private static final int ON_RECEIVED_CONNECT = ON_AUDIO_CHANGED + 1;
    private static final int ON_HANDUP_AGREE = ON_RECEIVED_CONNECT + 1;
    private static final int ON_HANDUP_REFUSE = ON_HANDUP_AGREE + 1;
    private static final int ON_RECEIVED_INVITE = ON_HANDUP_REFUSE + 1;
    private static final int ON_INVITE_AGREE = ON_RECEIVED_INVITE + 1;
    private static final int ON_INVITE_REFUSE = ON_INVITE_AGREE + 1;
    private static final int ON_ENTER_MIN_STATUS = ON_INVITE_REFUSE + 1;
    private static final int ON_ROOM_ERROR = ON_ENTER_MIN_STATUS + 1;
    private static final int ON_ROOM_CLOSED = ON_ROOM_ERROR + 1;
    private static final int ON_SDK_VIDEO_STATUS_CHANGED = ON_ROOM_CLOSED + 1;
    private static final int ON_ROOM_MESSAGE_RECEIVED = ON_SDK_VIDEO_STATUS_CHANGED + 1;

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
                    callback.onMemberJoin((Member) msg.obj);
                }
            } else if (msg.what == ON_MEMBER_LEAVE) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onMemberLeave((Member) msg.obj);
                }
            } else if (msg.what == ON_ROLE_CHANGED) {
                Bundle bundle = msg.getData();
                boolean isMine = bundle.getBoolean("isMine");
                Member member = (Member) bundle.getSerializable("member");

                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRoleChanged(isMine, member);
                }
            } else if (msg.what == ON_AUDIO_CHANGED) {
                Bundle bundle = msg.getData();
                boolean isMine = bundle.getBoolean("isMine");
                Member member = (Member) bundle.getSerializable("member");

                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onAudioStatusChanged(isMine, member);
                }
            } else if (msg.what == ON_RECEIVED_CONNECT) {
                Bundle bundle = msg.getData();
                Action.ACTION action = (Action.ACTION) bundle.getSerializable("action");
                Member member = (Member) bundle.getSerializable("member");

                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onReceivedRequest(member, action);
                }
            } else if (msg.what == ON_HANDUP_AGREE) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRequestAgreed((Member) msg.obj);
                }
            } else if (msg.what == ON_HANDUP_REFUSE) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRequestRefuse((Member) msg.obj);
                }
            } else if (msg.what == ON_RECEIVED_INVITE) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onReceivedInvite((Member) msg.obj);
                }
            } else if (msg.what == ON_INVITE_AGREE) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onInviteAgree((Member) msg.obj);
                }
            } else if (msg.what == ON_INVITE_REFUSE) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onInviteRefuse((Member) msg.obj);
                }
            } else if (msg.what == ON_ENTER_MIN_STATUS) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onEnterMinStatus();
                }
            } else if (msg.what == ON_ROOM_ERROR) {
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRoomError((Integer) msg.obj);
                }
            } else if (msg.what == ON_ROOM_CLOSED) {
                Bundle bundle = msg.getData();
                Room room = (Room) bundle.getSerializable("room");
                boolean fromUser = bundle.getBoolean("fromUser");
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRoomClosed(room, fromUser);
                }
            } else if (msg.what == ON_SDK_VIDEO_STATUS_CHANGED) {
                Member member = (Member) msg.obj;
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onSDKVideoStatusChanged(member);
                }
            } else if (msg.what == ON_ROOM_MESSAGE_RECEIVED) {
                Bundle bundle = msg.getData();
                String message = bundle.getString("message");
                Member member = (Member) bundle.getSerializable("member");
                for (RoomEventCallback callback : enevtCallbacks) {
                    callback.onRoomMessageReceived(member, message);
                }
            }
            return false;
        }
    });

    @Override
    public void onRoomClosed(@NonNull Room room, boolean fromUser) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("room", room);
        bundle.putBoolean("fromUser", fromUser);

        Message message = mHandler.obtainMessage(ON_ROOM_CLOSED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMemberJoin(@NonNull Member member) {
        mHandler.obtainMessage(ON_MEMBER_JOIN, member).sendToTarget();
    }

    @Override
    public void onMemberLeave(@NonNull Member member) {
        mHandler.obtainMessage(ON_MEMBER_LEAVE, member).sendToTarget();
    }

    @Override
    public void onRoleChanged(boolean isMine, @NonNull Member member) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("isMine", isMine);
        bundle.putSerializable("member", member);

        Message message = mHandler.obtainMessage(ON_ROLE_CHANGED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull Member member) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("isMine", isMine);
        bundle.putSerializable("member", member);

        Message message = mHandler.obtainMessage(ON_AUDIO_CHANGED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onSDKVideoStatusChanged(@NonNull Member member) {
        mHandler.obtainMessage(ON_SDK_VIDEO_STATUS_CHANGED, member).sendToTarget();
    }

    @Override
    public void onReceivedRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("action", action);
        bundle.putSerializable("member", member);

        Message message = mHandler.obtainMessage(ON_RECEIVED_CONNECT);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onRequestAgreed(@NonNull Member member) {
        mHandler.obtainMessage(ON_HANDUP_AGREE, member).sendToTarget();
    }

    @Override
    public void onRequestRefuse(@NonNull Member member) {
        mHandler.obtainMessage(ON_HANDUP_REFUSE, member).sendToTarget();
    }

    @Override
    public void onReceivedInvite(@NonNull Member member) {
        mHandler.obtainMessage(ON_RECEIVED_INVITE, member).sendToTarget();
    }

    @Override
    public void onInviteAgree(@NonNull Member member) {
        mHandler.obtainMessage(ON_INVITE_AGREE, member).sendToTarget();
    }

    @Override
    public void onInviteRefuse(@NonNull Member member) {
        mHandler.obtainMessage(ON_INVITE_REFUSE, member).sendToTarget();
    }

    @Override
    public void onEnterMinStatus() {
        mHandler.obtainMessage(ON_ENTER_MIN_STATUS).sendToTarget();
    }

    @Override
    public void onRoomError(int error) {
        mHandler.obtainMessage(ON_ROOM_ERROR, error).sendToTarget();
    }

    @Override
    public void onRoomMessageReceived(@NonNull Member member, @NonNull String message) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("message", message);
        bundle.putSerializable("member", member);

        Message message2 = mHandler.obtainMessage(ON_ROOM_MESSAGE_RECEIVED);
        message2.setData(bundle);
        message2.sendToTarget();
    }
}
