package com.agora.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.RoomEventCallback2;
import com.agora.data.manager.MainThreadDispatch2;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * 房间控制
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/06/01
 */
public final class RoomManager {
    private Logger.Builder mLogger = XLog.tag("RoomManager");

    private volatile static RoomManager instance;

    private Context mContext;
    private MainThreadDispatch2 mMainThreadDispatch = new MainThreadDispatch2();

    private Map<String, AgoraMember> memberHashMap = new ConcurrentHashMap<>();

    private volatile AgoraRoom mRoom;
    private volatile AgoraMember owner;
    private volatile AgoraMember mMine;

    private RoomManager(Context mContext) {
        this.mContext = mContext;
    }

    public static RoomManager Instance(Context mContext) {
        if (instance == null) {
            synchronized (RoomManager.class) {
                if (instance == null)
                    instance = new RoomManager(mContext.getApplicationContext());
            }
        }
        return instance;
    }

    @Nullable
    public AgoraMember getMine() {
        return mMine;
    }

    public void addRoomEventCallback(@NonNull RoomEventCallback2 callback) {
        mMainThreadDispatch.addRoomEventCallback(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback2 callback) {
        mMainThreadDispatch.removeRoomEventCallback(callback);
    }

    private void onMemberJoin(@NonNull AgoraMember member) {
        mLogger.d("onMemberJoin() called with: member = [%s]", member);
        memberHashMap.put(member.getId(), member);
        mMainThreadDispatch.onMemberJoin(member);
    }

    private void onMemberLeave(@NonNull AgoraMember member) {
        mLogger.d("onMemberLeave() called with: member = [%s]", member);
        mMainThreadDispatch.onMemberLeave(member);
        memberHashMap.remove(member.getId());
    }

    public void subcribeMemberEvent() {
        DocumentReference dcRoom = SyncManager.Instance()
                .collection(AgoraRoom.TABLE_NAME)
                .document(mRoom.getId());

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(AgoraMember.TABLE_NAME)
                .query(new Query().whereEqualTo(AgoraMember.COLUMN_ROOMID, dcRoom))
                .subcribe(new SyncManager.EventListener() {
                    @Override
                    public void onCreated(AgoraObject item) {
                        AgoraMember member = item.toObject(AgoraMember.class);
                        member.setId(item.getId());
                        onMemberJoin(member);
                    }

                    @Override
                    public void onUpdated(AgoraObject item) {
//                        AgoraMember member =memberHashMap.get(i)
//                        onMemberLeave(member);
                    }

                    @Override
                    public void onDeleted(String objectId) {
                        AgoraMember member = memberHashMap.get(objectId);
                        if (member == null) {
                            return;
                        }
                        onMemberLeave(member);
                    }

                    @Override
                    public void onSubscribeError(int error) {

                    }
                });
    }

    public Single<AgoraMember> joinRoom(AgoraRoom room) {
        return Single.create(emitter ->
                {
                    mLogger.d("joinRoom() called with: room = [%s]", room);
                    User mUser = UserManager.Instance(mContext).getUserLiveData().getValue();
                    if (mUser == null) {
                        emitter.onError(new NullPointerException("mUser is empty"));
                        return;
                    }

                    //1：判断房间是否存在
                    SyncManager.Instance()
                            .getRoom(room.getId())
                            .get(new SyncManager.DataItemCallback() {
                                @Override
                                public void onSuccess(AgoraObject result) {
                                    mRoom = result.toObject(AgoraRoom.class);
                                    mRoom.setId(result.getId());

                                    //2：删除一次，因为有可能是异常退出导致第二次进入，所以删除之前的。
                                    SyncManager.Instance()
                                            .getRoom(room.getId())
                                            .collection(AgoraMember.TABLE_NAME)
                                            .query(new Query()
                                                    .whereEqualTo(AgoraMember.COLUMN_USERID, mUser.getObjectId()))
                                            .delete(new SyncManager.Callback() {
                                                @Override
                                                public void onSuccess() {
                                                }

                                                @Override
                                                public void onFail(AgoraException exception) {
                                                    mLogger.e("delete() onFail() called with: exception = [%s]", exception.toString());
                                                }
                                            });

                                    //3. 加入到Member中
                                    mMine = new AgoraMember();
                                    mMine.setRoom(mRoom);
                                    mMine.setUserId(mUser.getObjectId());

                                    if (ObjectsCompat.equals(mUser.getObjectId(), mRoom.getOwnerId())) {
                                        mMine.setRole(AgoraMember.Role.Owner);
                                    } else {
                                        mMine.setRole(AgoraMember.Role.Listener);
                                    }

                                    SyncManager.Instance()
                                            .getRoom(room.getId())
                                            .collection(AgoraMember.TABLE_NAME)
                                            .add(mMine.toHashMap(), new SyncManager.DataItemCallback() {
                                                @Override
                                                public void onSuccess(AgoraObject result) {
                                                    mMine = result.toObject(AgoraMember.class);
                                                    mMine.setId(result.getId());
                                                    mMine.setRoom(mRoom);

                                                    if (ObjectsCompat.equals(mUser.getObjectId(), mRoom.getOwnerId())) {
                                                        owner = mMine;
                                                        subcribeMemberEvent();
                                                    } else {
                                                        owner = null;
                                                    }
                                                    emitter.onSuccess(mMine);
                                                }

                                                @Override
                                                public void onFail(AgoraException exception) {
                                                    mLogger.e("add() onFail() called with: exception = [%s]", exception.toString());
                                                    emitter.onError(exception);
                                                }
                                            });
                                }

                                @Override
                                public void onFail(AgoraException exception) {
                                    mLogger.e("getRoom() onFail() called with: exception = [%s]", exception.toString());
                                    emitter.onError(exception);
                                }
                            });
                }
        );
    }

    public Completable updateMineStreamId(AgoraRoom room, AgoraMember member) {
        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(room.getId())
                        .collection(AgoraMember.TABLE_NAME)
                        .document(member.getId())
                        .update(AgoraMember.COLUMN_STREAMID, member.getStreamId(), new SyncManager.DataItemCallback() {
                            @Override
                            public void onSuccess(AgoraObject result) {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        })
        );
    }

    public Completable leaveRoom() {
        if (mRoom == null) {
            return Completable.complete();
        }

        if (ObjectsCompat.equals(mMine, owner)) {
            //房主退出
            return Completable.create(emitter ->
            {
                DocumentReference dcRoom = SyncManager.Instance()
                        .collection(AgoraRoom.TABLE_NAME)
                        .document(mRoom.getId());
                SyncManager.Instance()
                        .collection(AgoraMember.TABLE_NAME)
                        .query(new Query().whereEqualTo(AgoraMember.COLUMN_ROOMID, dcRoom))
                        .delete(new SyncManager.Callback() {
                            @Override
                            public void onSuccess() {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        });

                SyncManager.Instance()
                        .getRoom(mRoom.getId())
                        .delete(new SyncManager.Callback() {
                            @Override
                            public void onSuccess() {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        });
            });
        } else {
            //观众退出
            return Completable.create(emitter ->
                    SyncManager.Instance()
                            .getRoom(mRoom.getId())
                            .collection(AgoraMember.TABLE_NAME)
                            .document(mMine.getId())
                            .delete(new SyncManager.Callback() {
                                @Override
                                public void onSuccess() {
                                    emitter.onComplete();
                                }

                                @Override
                                public void onFail(AgoraException exception) {
                                    emitter.onError(exception);
                                }
                            }));
        }
    }

    public Completable toggleSelfAudio(AgoraRoom room, AgoraMember member, boolean isMute) {
        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(room.getId())
                        .collection(AgoraMember.TABLE_NAME)
                        .document(member.getId())
                        .update(AgoraMember.COLUMN_ISSELFAUDIOMUTED, isMute ? 1 : 0, new SyncManager.DataItemCallback() {
                            @Override
                            public void onSuccess(AgoraObject result) {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        }));
    }

    public Completable toggleAudio(AgoraRoom room, AgoraMember member, boolean isMute) {
        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(room.getId())
                        .collection(AgoraMember.TABLE_NAME)
                        .document(member.getId())
                        .update(AgoraMember.COLUMN_ISAUDIOMUTED, isMute ? 1 : 0, new SyncManager.DataItemCallback() {
                            @Override
                            public void onSuccess(AgoraObject result) {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        }));
    }

    public Completable changeRole(AgoraRoom room, AgoraMember member, int role) {
        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(room.getId())
                        .collection(AgoraMember.TABLE_NAME)
                        .document(member.getId())
                        .update(AgoraMember.COLUMN_ROLE, role, new SyncManager.DataItemCallback() {
                            @Override
                            public void onSuccess(AgoraObject result) {
                                emitter.onComplete();
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        }));
    }
}
