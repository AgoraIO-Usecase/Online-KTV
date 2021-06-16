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
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

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

    public void loadMemberStatus() {
        for (AgoraMember value : memberHashMap.values()) {
            if (value.getRole() == AgoraMember.Role.Speaker) {
                onMemberRoleChanged(value);
            }
        }
    }

    @Nullable
    public AgoraRoom getRoom() {
        return mRoom;
    }

    @Nullable
    public AgoraMember getOwner() {
        return owner;
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

    private void onMemberRoleChanged(@NonNull AgoraMember member) {
        mLogger.d("onMemberRoleChanged() called with: member = [%s]", member);
        mMainThreadDispatch.onRoleChanged(false, member);
    }

    private void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
        mLogger.d("onAudioStatusChanged() called with: isMine = [%s], member = [%s]", isMine, member);
        mMainThreadDispatch.onAudioStatusChanged(isMine, member);
    }

    private void onMusicAdd(MusicModel model) {
//        mMainThreadDispatch.onAudioStatusChanged(isMine, member);
    }

    private void onMusicDelete(MusicModel model) {
//        mMainThreadDispatch.onAudioStatusChanged(isMine, member);
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
                        member.setRoom(mRoom);

                        onMemberJoin(member);
                    }

                    @Override
                    public void onUpdated(AgoraObject item) {
                        AgoraMember memberRemote = item.toObject(AgoraMember.class);
                        memberRemote.setId(item.getId());
                        memberRemote.setRoom(mRoom);

                        AgoraMember memberLocal = memberHashMap.get(memberRemote.getId());
                        if (memberLocal != null && memberLocal.getRole() != memberRemote.getRole()) {
                            memberLocal.setRole(memberRemote.getRole());
                            onMemberRoleChanged(memberLocal);
                        }

                        if (memberLocal != null && memberLocal.getIsSelfAudioMuted() != memberRemote.getIsSelfAudioMuted()) {
                            memberLocal.setIsSelfAudioMuted(memberRemote.getIsSelfAudioMuted());
                            onAudioStatusChanged(true, memberLocal);
                        }
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

    public void subcribeMusicEvent() {
        DocumentReference dcRoom = SyncManager.Instance()
                .collection(AgoraRoom.TABLE_NAME)
                .document(mRoom.getId());

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MusicModel.TABLE_NAME)
                .query(new Query().whereEqualTo(AgoraMember.COLUMN_ROOMID, dcRoom))
                .subcribe(new SyncManager.EventListener() {
                    @Override
                    public void onCreated(AgoraObject item) {
                        MusicModel data = item.toObject(MusicModel.class);
                        data.setId(item.getId());
                    }

                    @Override
                    public void onUpdated(AgoraObject item) {

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

    private List<MusicModel> musics = new ArrayList<>();

    private Single<List<MusicModel>> getMusicsFromRemote() {
        return Single.create(new SingleOnSubscribe<List<MusicModel>>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<List<MusicModel>> emitter) throws Exception {
                SyncManager.Instance()
                        .getRoom(mRoom.getId())
                        .collection(MusicModel.TABLE_NAME)
                        .get(new SyncManager.DataListCallback() {
                            @Override
                            public void onSuccess(List<AgoraObject> results) {
                                List<MusicModel> items = new ArrayList<>();
                                for (AgoraObject result : results) {
                                    MusicModel item = result.toObject(MusicModel.class);
                                    item.setId(result.getId());
                                    items.add(item);
                                }
                                emitter.onSuccess(items);
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        });
            }
        }).doOnSuccess(new Consumer<List<MusicModel>>() {
            @Override
            public void accept(List<MusicModel> musicModels) throws Exception {
                musics = musicModels;
            }
        });
    }

    public List<MusicModel> getMusics() {
        return musics;
    }

    public boolean isInMusicOrderList(MusicModel item) {
        return musics.contains(item);
    }

    private Single<AgoraRoom> getRoom(String id) {
        return Single.create(new SingleOnSubscribe<AgoraRoom>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<AgoraRoom> emitter) throws Exception {
                SyncManager.Instance()
                        .getRoom(id)
                        .get(new SyncManager.DataItemCallback() {
                            @Override
                            public void onSuccess(AgoraObject result) {
                                AgoraRoom mRoom = result.toObject(AgoraRoom.class);
                                mRoom.setId(result.getId());
                                emitter.onSuccess(mRoom);
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        });
            }
        });
    }

    private Single<AgoraMember> addMember(AgoraRoom room, AgoraMember member) {
        return Single.create(emitter -> {
            SyncManager.Instance()
                    .getRoom(room.getId())
                    .collection(AgoraMember.TABLE_NAME)
                    .add(member.toHashMap(), new SyncManager.DataItemCallback() {
                        @Override
                        public void onSuccess(AgoraObject result) {
                            AgoraMember member = result.toObject(AgoraMember.class);
                            member.setId(result.getId());
                            member.setRoom(room);
                            emitter.onSuccess(member);
                        }

                        @Override
                        public void onFail(AgoraException exception) {
                            emitter.onError(exception);
                        }
                    });
        });
    }

    private Completable deleteMember(String roomId, String userId) {
        return Completable.create(emitter -> {
            SyncManager.Instance()
                    .getRoom(roomId)
                    .collection(AgoraMember.TABLE_NAME)
                    .query(new Query()
                            .whereEqualTo(AgoraMember.COLUMN_USERID, userId))
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
    }

    public Completable joinRoom(AgoraRoom room) {
        return Completable.create(emitter ->
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

                                                    //4. 获取当前成员列表
                                                    SyncManager.Instance()
                                                            .getRoom(mRoom.getId())
                                                            .collection(AgoraMember.TABLE_NAME)
                                                            .get(new SyncManager.DataListCallback() {
                                                                @Override
                                                                public void onSuccess(List<AgoraObject> results) {
                                                                    List<AgoraMember> members = new ArrayList<>();
                                                                    for (AgoraObject result : results) {
                                                                        AgoraMember member = result.toObject(AgoraMember.class);
                                                                        member.setId(result.getId());
                                                                        members.add(member);

                                                                        memberHashMap.put(member.getId(), member);

                                                                        if (ObjectsCompat.equals(member, mMine)) {
                                                                            mMine = member;
                                                                        }

                                                                        if (ObjectsCompat.equals(member.getUserId(), mRoom.getOwnerId())) {
                                                                            owner = member;
                                                                        }
                                                                    }

                                                                    emitter.onComplete();
                                                                }

                                                                @Override
                                                                public void onFail(AgoraException exception) {
                                                                    mLogger.e("getMembers() onFail() called with: exception = [%s]", exception.toString());
                                                                    emitter.onError(exception);
                                                                }
                                                            });
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
        ).doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                subcribeMemberEvent();
            }
        });
    }

    private Single<List<AgoraMember>> getMembers() {
        return Single.create(new SingleOnSubscribe<List<AgoraMember>>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<List<AgoraMember>> emitter) throws Exception {
                SyncManager.Instance()
                        .getRoom(mRoom.getId())
                        .collection(AgoraMember.TABLE_NAME)
                        .get(new SyncManager.DataListCallback() {
                            @Override
                            public void onSuccess(List<AgoraObject> results) {
                                List<AgoraMember> members = new ArrayList<>();
                                for (AgoraObject result : results) {
                                    AgoraMember member = result.toObject(AgoraMember.class);
                                    member.setId(result.getId());
                                    members.add(member);

                                    memberHashMap.put(member.getId(), member);
                                }

                                emitter.onSuccess(members);
                            }

                            @Override
                            public void onFail(AgoraException exception) {
                                emitter.onError(exception);
                            }
                        });
            }
        });
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

    public Completable toggleSelfAudio(boolean isMute) {
        if (mRoom == null) {
            return Completable.complete();
        }

        AgoraMember mMine = getMine();
        if (mMine == null) {
            return Completable.complete();
        }

        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(mRoom.getId())
                        .collection(AgoraMember.TABLE_NAME)
                        .document(mMine.getId())
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

    public Completable toggleAudio(AgoraMember member, boolean isMute) {
        if (mRoom == null) {
            return Completable.complete();
        }

        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(mRoom.getId())
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

    public Completable changeRole(AgoraMember member, int role) {
        if (mRoom == null) {
            return Completable.complete();
        }

        return Completable.create(emitter ->
                SyncManager.Instance()
                        .getRoom(mRoom.getId())
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
