package io.agora.ktv.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.BaseError;
import com.agora.data.R;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.observer.DataObserver;
import com.agora.data.provider.AgoraObject;
import com.agora.data.provider.DataRepositroy;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.DocumentReference;
import com.agora.data.sync.OrderBy;
import com.agora.data.sync.Query;
import com.agora.data.sync.SyncManager;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.ktv.bean.MusicModel;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
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
    private Logger.Builder mLoggerRTC = XLog.tag("RTC");

    private volatile static RoomManager instance;

    private Context mContext;
    private MainThreadDispatch mMainThreadDispatch = new MainThreadDispatch();

    private Map<String, AgoraMember> memberHashMap = new ConcurrentHashMap<>();

    private volatile AgoraRoom mRoom;
    private volatile AgoraMember owner;
    private volatile AgoraMember mMine;

    private volatile MusicModel mMusicModel;

    private IMediaPlayer mPlayer;
    private RtcEngine mRtcEngine;

    private IRtcEngineEventHandler mIRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            super.onConnectionStateChanged(state, reason);
            mLogger.d("onConnectionStateChanged() called with: state = [%s], reason = [%s]", state, reason);

            if (state == Constants.CONNECTION_STATE_FAILED) {
                if (emitterJoinRTC != null) {
                    emitterJoinRTC.onError(new Exception("connection_state_failed"));
                    emitterJoinRTC = null;
                }
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            mLoggerRTC.d("onJoinChannelSuccess() called with: channel = [%s], uid = [%s], elapsed = [%s]", channel, uid, elapsed);

            if (emitterJoinRTC != null) {
                emitterJoinRTC.onComplete();
                emitterJoinRTC = null;
            }
            mMainThreadDispatch.onRTCJoinRoom();
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            mLoggerRTC.d("onLeaveChannel() called with: stats = [%s]", stats);
        }

        @Override
        public void onRemoteAudioStateChanged(int uid, REMOTE_AUDIO_STATE state, REMOTE_AUDIO_STATE_REASON reason, int elapsed) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
            mLoggerRTC.d("onRemoteAudioStateChanged() called with: uid = [%s], state = [%s], reason = [%s], elapsed = [%s]", uid, state, reason, elapsed);
        }

        @Override
        public void onLocalAudioStateChanged(LOCAL_AUDIO_STREAM_STATE state, LOCAL_AUDIO_STREAM_ERROR error) {
            super.onLocalAudioStateChanged(state, error);
            mLoggerRTC.d("onLocalAudioStateChanged() called with: state = [%s], error = [%s]", state, error);
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            JSONObject jsonMsg;
            try {
                String strMsg = new String(data);
                jsonMsg = new JSONObject(strMsg);
                if (mMusicModel == null)
                    return;

                if (jsonMsg.getString("cmd").equals("setLrcTime")) {
                    if (!jsonMsg.getString("lrcId").equals(mMusicModel.getMusicId())) {
                        return;
                    }

                    long total = jsonMsg.getLong("duration");
                    long cur = jsonMsg.getLong("time");
                    mMainThreadDispatch.onMusicProgress(total, cur);
                } else if (jsonMsg.getString("cmd").equals("musicStopped")) {
//                    stopDisplayLrc();
                }
            } catch (JSONException exp) {
                exp.printStackTrace();
            }
        }
    };

    private RoomManager(Context mContext) {
        this.mContext = mContext;
        iniRTC();
    }

    private void iniRTC() {
        String appid = mContext.getString(R.string.app_id);
        if (TextUtils.isEmpty(appid)) {
            throw new NullPointerException("please check \"strings_config.xml\"");
        }

        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = mContext;
        config.mAppId = appid;
        config.mEventHandler = mIRtcEngineEventHandler;
        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
//        if (Config.isLeanCloud()) {
//            config.mAreaCode = RtcEngineConfig.AreaCode.AREA_CODE_CN;
//        } else {
//            config.mAreaCode = RtcEngineConfig.AreaCode.AREA_CODE_GLOB;
//        }

        try {
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            e.printStackTrace();
            mLogger.e("init error", e);
        }
    }

    public RtcEngine getRtcEngine() {
        return mRtcEngine;
    }

    public IMediaPlayer getPlayer() {
        if (mPlayer == null) {
            mPlayer = mRtcEngine.createMediaPlayer();
        }
        return mPlayer;
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

    public boolean isOwner() {
        return ObjectsCompat.equals(getOwner(), getMine());
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

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.addRoomEventCallback(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.removeRoomEventCallback(callback);
    }

    private void onMemberJoin(@NonNull AgoraMember member) {
        mLogger.d("onMemberJoin() called with: member = [%s]", member);
        memberHashMap.put(member.getId(), member);
        mMainThreadDispatch.onMemberJoin(member);

        DataRepositroy.Instance(mContext)
                .getUser(member.getUserId())
                .subscribe(new DataObserver<User>(mContext) {
                    @Override
                    public void handleError(@NonNull BaseError e) {

                    }

                    @Override
                    public void handleSuccess(@NonNull User user) {
                        member.setUser(user);
                        mMainThreadDispatch.onMemberJoin(member);
                    }
                });
    }

    private void onMemberLeave(@NonNull AgoraMember member) {
        mLogger.d("onMemberLeave() called with: member = [%s]", member);
        mMainThreadDispatch.onMemberLeave(member);
        memberHashMap.remove(member.getId());
    }

    private void onMemberRoleChanged(@NonNull AgoraMember member) {
        mLogger.d("onMemberRoleChanged() called with: member = [%s]", member);
        mMainThreadDispatch.onRoleChanged(member);
    }

    private void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
        mLogger.d("onAudioStatusChanged() called with: isMine = [%s], member = [%s]", isMine, member);
        mMainThreadDispatch.onAudioStatusChanged(isMine, member);
    }

    private void onMusicAdd(MusicModel model) {
        mLogger.d("onMusicAdd() called with: model = [%s]", model);
        synchronized (musicObject) {
            if (musics.contains(model)) {
                return;
            }

            musics.add(model);

            mMainThreadDispatch.onMusicAdd(model);

            if (mMusicModel == null) {
                onMusicChanged(musics.get(0));
            }
        }
    }

    private void onMusicDelete(MusicModel model) {
        mLogger.d("onMusicDelete() called with: model = [%s]", model);
        musics.remove(model);
        mMainThreadDispatch.onMusicDelete(model);

        if (musics.size() > 0) {
            onMusicChanged(musics.get(0));
        } else {
            onMusicEmpty();
        }
    }

    public void onMusicEmpty() {
        mLogger.d("onMusicEmpty() called");
        mMusicModel = null;
        mMainThreadDispatch.onMusicEmpty();
    }

    public void onMusicChanged(MusicModel model) {
        mLogger.d("onMusicChanged() called with: model = [%s]", model);
        mMusicModel = model;
        mMainThreadDispatch.onMusicChanged(model);
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
        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MusicModel.TABLE_NAME)
                .query(new Query().whereEqualTo(AgoraMember.COLUMN_ROOMID, mRoom.getId()))
                .subcribe(new SyncManager.EventListener() {
                    @Override
                    public void onCreated(AgoraObject item) {
                        MusicModel data = item.toObject(MusicModel.class);
                        data.setId(item.getId());
                        onMusicAdd(data);
                    }

                    @Override
                    public void onUpdated(AgoraObject item) {

                    }

                    @Override
                    public void onDeleted(String objectId) {
                        synchronized (musicObject) {
                            MusicModel musicLocal = null;
                            for (MusicModel music : musics) {
                                if (ObjectsCompat.equals(objectId, music.getId())) {
                                    musicLocal = music;
                                    break;
                                }
                            }

                            if (musicLocal == null) {
                                return;
                            }

                            onMusicDelete(musicLocal);
                        }
                    }

                    @Override
                    public void onSubscribeError(int error) {

                    }
                });
    }

    private final static Object musicObject = new Object();
    private volatile List<MusicModel> musics = new ArrayList<>();

    public Single<List<MusicModel>> getMusicsFromRemote() {
        return Single.create(new SingleOnSubscribe<List<MusicModel>>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<List<MusicModel>> emitter) throws Exception {
                SyncManager.Instance()
                        .getRoom(mRoom.getId())
                        .collection(MusicModel.TABLE_NAME)
                        .query(new Query()
                                .orderBy(OrderBy.getInstance(OrderBy.Direction.ASCENDING, MusicModel.COLUMN_CREATE))
                                .whereEqualTo(MusicModel.COLUMN_ROOMID, mRoom.getId()))
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
                synchronized (musicObject) {
                    musics = musicModels;
                }
            }
        });
    }

    @Nullable
    public MusicModel getMusicModel() {
        return mMusicModel;
    }

    public List<MusicModel> getMusics() {
        return musics;
    }

    public boolean isInMusicOrderList(MusicModel item) {
        return musics.contains(item);
    }

    private Single<AgoraMember> preJoinAddMember(AgoraRoom room, AgoraMember member) {
        return Single.create((SingleOnSubscribe<AgoraMember>) emitter -> {
            SyncManager.Instance()
                    .getRoom(room.getId())
                    .collection(AgoraMember.TABLE_NAME)
                    .add(member.toHashMap(), new SyncManager.DataItemCallback() {
                        @Override
                        public void onSuccess(AgoraObject result) {
                            AgoraMember memberTemp = result.toObject(AgoraMember.class);
                            AgoraObject roomObject = (AgoraObject) result.get(AgoraMember.COLUMN_ROOMID);

                            AgoraRoom mRoom = roomObject.toObject(AgoraRoom.class);
                            mRoom.setId(roomObject.getId());

                            memberTemp.setId(result.getId());
                            memberTemp.setRoom(room);
                            memberTemp.setUser(member.getUser());
                            emitter.onSuccess(memberTemp);
                        }

                        @Override
                        public void onFail(AgoraException exception) {
                            emitter.onError(exception);
                        }
                    });
        }).doOnSuccess(new Consumer<AgoraMember>() {
            @Override
            public void accept(AgoraMember member) throws Exception {
                mMine = member;
            }
        });
    }

    private Completable preJoinDeleteMember(String roomId, String userId) {
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
        this.mRoom = room;

        User mUser = UserManager.Instance(mContext).getUserLiveData().getValue();
        if (mUser == null) {
            return Completable.error(new NullPointerException("mUser is empty"));
        }

        mMine = new AgoraMember();
        mMine.setRoom(mRoom);
        mMine.setUserId(mUser.getObjectId());
        mMine.setUser(mUser);

        if (ObjectsCompat.equals(mUser.getObjectId(), mRoom.getOwnerId())) {
            mMine.setRole(AgoraMember.Role.Owner);
        } else {
            mMine.setRole(AgoraMember.Role.Listener);
        }

        return preJoinDeleteMember(room.getId(), mUser.getObjectId())
                .andThen(preJoinAddMember(mRoom, mMine).ignoreElement())
                .andThen(preJoinSyncMembers().ignoreElement())
                .andThen(joinRTC())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        mMusicModel = null;
                        subcribeMemberEvent();
                        subcribeMusicEvent();
                    }
                });
    }

    private CompletableEmitter emitterJoinRTC = null;

    private Completable joinRTC() {
        return Completable.create(emitter -> {
            emitterJoinRTC = emitter;
            getRtcEngine().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            getRtcEngine().enableAudio();
            if (ObjectsCompat.equals(mMine, owner)) {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            } else if (mMine.getRole() == AgoraMember.Role.Speaker) {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            } else {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            }

            mLogger.i("joinRTC() called with: results = [%s]", mRoom);
            int ret = getRtcEngine().joinChannel("", mRoom.getId(), null, 0);
            if (ret != Constants.ERR_OK) {
                mLogger.e("joinRTC() called error " + ret);
                emitter.onError(new Exception("join rtc room error " + ret));
                emitterJoinRTC = null;
            }
        });
    }

    private Single<List<AgoraMember>> preJoinSyncMembers() {
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
                                    final AgoraMember member = result.toObject(AgoraMember.class);
                                    member.setId(result.getId());
                                    member.setRoom(mRoom);
                                    members.add(member);

                                    memberHashMap.put(member.getId(), member);

                                    if (ObjectsCompat.equals(member, mMine)) {
                                        mMine = member;
                                        User mUser = UserManager.Instance(mContext).getUserLiveData().getValue();
                                        mMine.setUser(mUser);
                                    }

                                    if (ObjectsCompat.equals(member.getUserId(), mRoom.getOwnerId())) {
                                        owner = member;
                                    }

                                    DataRepositroy.Instance(mContext)
                                            .getUser(member.getUserId())
                                            .subscribe(new DataObserver<User>(mContext) {
                                                @Override
                                                public void handleError(@NonNull BaseError e) {

                                                }

                                                @Override
                                                public void handleSuccess(@NonNull User user) {
                                                    member.setUser(user);
                                                }
                                            });
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

        mLogger.d("leaveRoom() called");
        getRtcEngine().leaveChannel();

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
