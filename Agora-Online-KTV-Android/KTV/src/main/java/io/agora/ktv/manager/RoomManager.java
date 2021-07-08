package io.agora.ktv.manager;

import android.content.Context;
import android.os.Handler;
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

import io.agora.ktv.bean.MemberMusicModel;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
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

    private volatile static AgoraRoom mRoom;
    private volatile static AgoraMember owner;
    private volatile static AgoraMember mMine;

    private volatile MemberMusicModel mMusicModel;

    private RtcEngine mRtcEngine;

    private IRtcEngineEventHandler mIRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            super.onConnectionStateChanged(state, reason);
            mLoggerRTC.d("onConnectionStateChanged() called with: state = [%s], reason = [%s]", state, reason);

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
            mLoggerRTC.i("onJoinChannelSuccess() called with: channel = [%s], uid = [%s], elapsed = [%s]", channel, uid, elapsed);

            if (emitterJoinRTC != null) {
                emitterJoinRTC.onSuccess(uid);
                emitterJoinRTC = null;
            }
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            mLoggerRTC.i("onLeaveChannel() called with: stats = [%s]", stats);
        }

        @Override
        public void onRemoteAudioStateChanged(int uid, REMOTE_AUDIO_STATE state, REMOTE_AUDIO_STATE_REASON reason, int elapsed) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
            mLoggerRTC.i("onRemoteAudioStateChanged() called with: uid = [%s], state = [%s], reason = [%s], elapsed = [%s]", uid, state, reason, elapsed);
        }

        @Override
        public void onLocalAudioStateChanged(LOCAL_AUDIO_STREAM_STATE state, LOCAL_AUDIO_STREAM_ERROR error) {
            super.onLocalAudioStateChanged(state, error);
            mLoggerRTC.i("onLocalAudioStateChanged() called with: state = [%s], error = [%s]", state, error);
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
            mLoggerRTC.e("init error", e);
        }
    }

    public RtcEngine getRtcEngine() {
        return mRtcEngine;
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
        mLogger.i("onMemberJoin() called with: member = [%s]", member);
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
        mLogger.i("onMemberLeave() called with: member = [%s]", member);
        mMainThreadDispatch.onMemberLeave(member);
        memberHashMap.remove(member.getId());
    }

    private void onMemberRoleChanged(@NonNull AgoraMember member) {
        mLogger.i("onMemberRoleChanged() called with: member = [%s]", member);
        mMainThreadDispatch.onRoleChanged(member);
    }

    private void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
        mLogger.i("onAudioStatusChanged() called with: isMine = [%s], member = [%s]", isMine, member);
        mMainThreadDispatch.onAudioStatusChanged(isMine, member);
    }

    private void onMusicAdd(MemberMusicModel model) {
        mLogger.i("onMusicAdd() called with: model = [%s]", model);
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

    private void onMusicDelete(MemberMusicModel model) {
        mLogger.i("onMusicDelete() called with: model = [%s]", model);
        musics.remove(model);
        mMainThreadDispatch.onMusicDelete(model);

        if (musics.size() > 0) {
            onMusicChanged(musics.get(0));
        } else {
            onMusicEmpty();
        }
    }

    public void onMusicEmpty() {
        mLogger.i("onMusicEmpty() called");
        mMusicModel = null;
        mMainThreadDispatch.onMusicEmpty();
    }

    public void onMusicChanged(MemberMusicModel model) {
        mLogger.i("onMusicChanged() called with: model = [%s]", model);
        mMusicModel = model;
        mMainThreadDispatch.onMusicChanged(model);
    }

    private SyncManager.EventListener mRoomEvent = new SyncManager.EventListener() {

        @Override
        public void onCreated(AgoraObject item) {

        }

        @Override
        public void onUpdated(AgoraObject item) {
            AgoraRoom room = item.toObject(AgoraRoom.class);
            room.setId(item.getId());

            if (ObjectsCompat.equals(room, mRoom)) {
                mRoom = room;
                mMainThreadDispatch.onRoomInfoChanged(mRoom);
            }
        }

        @Override
        public void onDeleted(String objectId) {

        }

        @Override
        public void onSubscribeError(AgoraException ex) {
            mLogger.e("mRoomEvent() called with: ex = [%s]", String.valueOf(ex));
            if (ex.getCode() == AgoraException.ERROR_LEANCLOULD_OVER_COUNT) {
                mMainThreadDispatch.onRoomError(ex.getCode(), "LeanCloud异常，超过开发版应用的每天限额，请购买商业版");
            } else {
                mMainThreadDispatch.onRoomError(ex.getCode(), String.format("LeanCloud异常，错误码：%s", ex.getCode()));
            }
        }
    };

    public void subcribeRoomEvent() {
        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .query(new Query().whereEqualTo(AgoraRoom.COLUMN_ID, mRoom.getId()))
                .subcribe(mRoomEvent);
    }

    private SyncManager.EventListener mMemberEvent = new SyncManager.EventListener() {
        @Override
        public void onCreated(AgoraObject item) {
            AgoraMember member = item.toObject(AgoraMember.class);
            member.setId(item.getId());
            member.setRoomId(mRoom);

            onMemberJoin(member);
        }

        @Override
        public void onUpdated(AgoraObject item) {
            AgoraMember memberRemote = item.toObject(AgoraMember.class);
            memberRemote.setId(item.getId());
            memberRemote.setRoomId(mRoom);

            AgoraMember memberLocal = memberHashMap.get(memberRemote.getId());
            if (memberLocal != null && memberLocal.getRole() != memberRemote.getRole()) {
                memberLocal.setRole(memberRemote.getRole());
                onMemberRoleChanged(memberLocal);
            }

            if (memberLocal != null && memberLocal.getIsSelfMuted() != memberRemote.getIsSelfMuted()) {
                memberLocal.setIsSelfMuted(memberRemote.getIsSelfMuted());
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
        public void onSubscribeError(AgoraException ex) {
            mLogger.e("mMemberEvent() called with: ex = [%s]", String.valueOf(ex));
            if (ex.getCode() == AgoraException.ERROR_LEANCLOULD_OVER_COUNT) {
                mMainThreadDispatch.onRoomError(ex.getCode(), "LeanCloud异常，超过开发版应用的每天限额，请购买商业版");
            } else {
                mMainThreadDispatch.onRoomError(ex.getCode(), String.format("LeanCloud异常，错误码：%s", ex.getCode()));
            }
        }
    };

    public void subcribeMemberEvent() {
        DocumentReference dcRoom = SyncManager.Instance()
                .collection(AgoraRoom.TABLE_NAME)
                .document(mRoom.getId());

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(AgoraMember.TABLE_NAME)
                .query(new Query().whereEqualTo(AgoraMember.COLUMN_ROOMID, dcRoom))
                .subcribe(mMemberEvent);
    }

    private SyncManager.EventListener mMusicEvent = new SyncManager.EventListener() {
        @Override
        public void onCreated(AgoraObject item) {
            MemberMusicModel data = item.toObject(MemberMusicModel.class);
            data.setId(item.getId());
            onMusicAdd(data);
        }

        @Override
        public void onUpdated(AgoraObject item) {

        }

        @Override
        public void onDeleted(String objectId) {
            synchronized (musicObject) {
                MemberMusicModel musicLocal = null;
                for (MemberMusicModel music : musics) {
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
        public void onSubscribeError(AgoraException ex) {
            mLogger.e("mMusicEvent() called with: ex = [%s]", String.valueOf(ex));
            if (ex.getCode() == AgoraException.ERROR_LEANCLOULD_OVER_COUNT) {
                mMainThreadDispatch.onRoomError(ex.getCode(), "LeanCloud异常，超过开发版应用的每天限额，请购买商业版");
            } else {
                mMainThreadDispatch.onRoomError(ex.getCode(), String.format("LeanCloud异常，错误码：%s", ex.getCode()));
            }
        }
    };

    public void subcribeMusicEvent() {
        DocumentReference drRoom = SyncManager.Instance()
                .collection(AgoraRoom.TABLE_NAME)
                .document(mRoom.getId());

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MemberMusicModel.TABLE_NAME)
                .query(new Query().whereEqualTo(AgoraMember.COLUMN_ROOMID, drRoom))
                .subcribe(mMusicEvent);
    }

    private final static Object musicObject = new Object();
    private volatile List<MemberMusicModel> musics = new ArrayList<>();

    public Single<List<MemberMusicModel>> getMusicOrderList() {
        return Single.create(new SingleOnSubscribe<List<MemberMusicModel>>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<List<MemberMusicModel>> emitter) throws Exception {
                DocumentReference drRoom = SyncManager.Instance()
                        .collection(AgoraRoom.TABLE_NAME)
                        .document(mRoom.getId());

                SyncManager.Instance()
                        .getRoom(mRoom.getId())
                        .collection(MemberMusicModel.TABLE_NAME)
                        .query(new Query()
                                .orderBy(OrderBy.getInstance(OrderBy.Direction.ASCENDING, MemberMusicModel.COLUMN_CREATE))
                                .whereEqualTo(MemberMusicModel.COLUMN_ROOMID, drRoom))
                        .get(new SyncManager.DataListCallback() {
                            @Override
                            public void onSuccess(List<AgoraObject> results) {
                                List<MemberMusicModel> items = new ArrayList<>();
                                for (AgoraObject result : results) {
                                    MemberMusicModel item = result.toObject(MemberMusicModel.class);
                                    item.setRoomId(mRoom);
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
        }).doOnSuccess(new Consumer<List<MemberMusicModel>>() {
            @Override
            public void accept(List<MemberMusicModel> musicModels) throws Exception {
                synchronized (musicObject) {
                    musics = musicModels;
                }
            }
        });
    }

    @Nullable
    public MemberMusicModel getMusicModel() {
        return mMusicModel;
    }

    public List<MemberMusicModel> getMusics() {
        return musics;
    }

    public boolean isInMusicOrderList(MemberMusicModel item) {
        return musics.contains(item);
    }

    private Single<AgoraMember> preJoinAddMember(AgoraRoom room, AgoraMember member) {
        return Single.create((SingleOnSubscribe<AgoraMember>) emitter -> {
            mLogger.i("preJoinAddMember() called with: room = [%s], member = [%s]", room, member);
            SyncManager.Instance()
                    .getRoom(room.getId())
                    .collection(AgoraMember.TABLE_NAME)
                    .query(new Query().whereEqualTo(AgoraMember.COLUMN_USERID, member.getUserId()))
                    .get(new SyncManager.DataListCallback() {
                        @Override
                        public void onSuccess(List<AgoraObject> result) {
                            if (result == null || result.size() <= 0) {
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
                                                memberTemp.setRoomId(room);
                                                memberTemp.setUser(member.getUser());
                                                emitter.onSuccess(memberTemp);
                                            }

                                            @Override
                                            public void onFail(AgoraException exception) {
                                                emitter.onError(exception);
                                            }
                                        });
                                return;
                            }

                            AgoraObject resultFirst = result.get(0);
                            AgoraMember memberTemp = resultFirst.toObject(AgoraMember.class);
                            AgoraObject roomObject = (AgoraObject) resultFirst.get(AgoraMember.COLUMN_ROOMID);

                            AgoraRoom mRoom = roomObject.toObject(AgoraRoom.class);
                            mRoom.setId(roomObject.getId());

                            memberTemp.setId(resultFirst.getId());
                            memberTemp.setRoomId(room);
                            memberTemp.setUser(member.getUser());
                            emitter.onSuccess(memberTemp);
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
        mMine.setRoomId(mRoom);
        mMine.setUserId(mUser.getObjectId());
        mMine.setUser(mUser);

        if (ObjectsCompat.equals(mUser.getObjectId(), mRoom.getUserId())) {
            mMine.setRole(AgoraMember.Role.Owner);
        } else {
            mMine.setRole(AgoraMember.Role.Listener);
        }

        return preJoinAddMember(mRoom, mMine).doOnSuccess(member -> mMine = member).ignoreElement()
                .andThen(preJoinSyncMembers().ignoreElement())
                .andThen(joinRTC().doOnSuccess(uid -> {
                    Long streamId = uid & 0xffffffffL;
                    mMine.setStreamId(streamId);
                }).ignoreElement())
                .andThen(updateMineStreamId())
                .doOnComplete(() -> onJoinRoom());
    }

    private void onJoinRoom() {
        mMusicModel = null;

        //LeanCloud连续订阅会有问题，需要做延迟
        subcribeRoomEvent();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mRoom == null) {
                    return;
                }
                subcribeMemberEvent();
            }
        }, 200L);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mRoom == null) {
                    return;
                }
                subcribeMusicEvent();
            }
        }, 400L);
    }

    private SingleEmitter<Integer> emitterJoinRTC = null;

    private Single<Integer> joinRTC() {
        return Single.create(emitter -> {
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

            mLoggerRTC.i("joinRTC() called with: results = [%s]", mRoom);
            int ret = getRtcEngine().joinChannel("", mRoom.getId(), null, 0);
            if (ret != Constants.ERR_OK) {
                mLoggerRTC.e("joinRTC() called error " + ret);
                emitter.onError(new Exception("join rtc room error " + ret));
                emitterJoinRTC = null;
            }
        });
    }

    private Single<List<AgoraMember>> preJoinSyncMembers() {
        return Single.create(emitter -> {
            mLogger.i("preJoinSyncMembers() called");

            DocumentReference dcRoom = SyncManager.Instance()
                    .collection(AgoraRoom.TABLE_NAME)
                    .document(mRoom.getId());

            SyncManager.Instance()
                    .getRoom(mRoom.getId())
                    .collection(AgoraMember.TABLE_NAME)
                    .query(new Query().whereEqualTo(AgoraMember.COLUMN_ROOMID, dcRoom))
                    .get(new SyncManager.DataListCallback() {
                        @Override
                        public void onSuccess(List<AgoraObject> results) {
                            List<AgoraMember> members = new ArrayList<>();
                            for (AgoraObject result : results) {
                                final AgoraMember member = result.toObject(AgoraMember.class);
                                member.setId(result.getId());
                                member.setRoomId(mRoom);
                                members.add(member);

                                memberHashMap.put(member.getId(), member);

                                if (ObjectsCompat.equals(member, mMine)) {
                                    mMine = member;
                                    User mUser = UserManager.Instance(mContext).getUserLiveData().getValue();
                                    mMine.setUser(mUser);
                                }

                                if (ObjectsCompat.equals(member.getUserId(), mRoom.getUserId())) {
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
        });
    }

    private Completable updateMineStreamId() {
        return Completable.create(emitter ->
                {
                    mLogger.d("updateMineStreamId() called with mMine= [%s]", mMine);
                    SyncManager.Instance()
                            .getRoom(mRoom.getId())
                            .collection(AgoraMember.TABLE_NAME)
                            .document(mMine.getId())
                            .update(AgoraMember.COLUMN_STREAMID, mMine.getStreamId(), new SyncManager.DataItemCallback() {
                                @Override
                                public void onSuccess(AgoraObject result) {
                                    emitter.onComplete();
                                }

                                @Override
                                public void onFail(AgoraException exception) {
                                    emitter.onError(exception);
                                }
                            });
                }
        );
    }

    public Completable leaveRoom() {
        mLogger.i("leaveRoom() called");
        if (mRoom == null) {
            return Completable.complete();
        }

        SyncManager.Instance().unsubcribe(mRoomEvent);
        SyncManager.Instance().unsubcribe(mMemberEvent);
        SyncManager.Instance().unsubcribe(mMusicEvent);

        mLoggerRTC.i("leaveChannel() called");
        getRtcEngine().leaveChannel();

        memberHashMap.clear();

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
            }).doOnComplete(() -> {
                mRoom = null;
                mMine = null;
                owner = null;
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
                            }))
                    .doOnComplete(() -> {
                        mRoom = null;
                        mMine = null;
                        owner = null;
                    });
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
