package com.agora.data.sync;

import android.content.Context;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

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

    public void subcribeRoom(AgoraRoom room) {
        SyncManager.Instance()
                .getRoom(room.getId())
                .subcribe(new SyncManager.EventListener() {
                    @Override
                    public void onCreated(AgoraObject item) {

                    }

                    @Override
                    public void onUpdated(AgoraObject item) {

                    }

                    @Override
                    public void onDeleted(String objectId) {

                    }

                    @Override
                    public void onSubscribeError(int error) {

                    }
                });
    }

    public void subcribeRoom2(AgoraRoom room) {
        SyncManager.Instance()
                .getRoom(room.getId())
                .collection(AgoraMember.TABLE_NAME)
                .document("111")
                .subcribe(new SyncManager.EventListener() {
                    @Override
                    public void onCreated(AgoraObject item) {

                    }

                    @Override
                    public void onUpdated(AgoraObject item) {

                    }

                    @Override
                    public void onDeleted(String objectId) {

                    }

                    @Override
                    public void onSubscribeError(int error) {

                    }
                });
    }

    public Single<AgoraMember> joinRoom(AgoraRoom room, User user) {
        return Single.create(emitter ->
                {
                    mLogger.d("joinRoom() called with: room = [%s], user = [%s]", room, user);
                    //1：判断房间是否存在
                    SyncManager.Instance()
                            .getRoom(room.getId())
                            .get(new SyncManager.DataItemCallback() {
                                @Override
                                public void onSuccess(AgoraObject result) {
                                    mLogger.d("getRoom() onSuccess() called with: result = [%s]", result);
                                    AgoraRoom mRoom = result.toObject(AgoraRoom.class);

                                    //2：删除一次，因为有可能是异常退出导致第二次进入，所以删除之前的。
                                    SyncManager.Instance()
                                            .getRoom(room.getId())
                                            .collection(AgoraMember.TABLE_NAME)
                                            .query(new Query()
                                                    .whereEqualTo(AgoraMember.COLUMN_USERID, user.getObjectId()))
                                            .delete(new SyncManager.Callback() {
                                                @Override
                                                public void onSuccess() {
                                                    mLogger.d("delete() onSuccess() called");
                                                }

                                                @Override
                                                public void onFail(AgoraException exception) {
                                                    mLogger.e("delete() onFail() called with: exception = [%s]", exception.toString());
                                                }
                                            });

                                    //3. 加入到Member中
                                    AgoraMember agoraMember = new AgoraMember();
                                    agoraMember.setRoomId(room.getId());
                                    agoraMember.setUserId(user.getObjectId());

                                    SyncManager.Instance()
                                            .getRoom(room.getId())
                                            .collection(AgoraMember.TABLE_NAME)
                                            .add(agoraMember.toHashMap(), new SyncManager.DataItemCallback() {
                                                @Override
                                                public void onSuccess(AgoraObject result) {
                                                    mLogger.d("add() onSuccess() called with: result = [%s]", result);
                                                    AgoraMember agoraMemberRemote = result.toObject(AgoraMember.class);
                                                    agoraMemberRemote.setId(result.getId());
                                                    emitter.onSuccess(agoraMemberRemote);
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

    public Completable leaveRoom(AgoraRoom room) {
        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(room.getId())
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
