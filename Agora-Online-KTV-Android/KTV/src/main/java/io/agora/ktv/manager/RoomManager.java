package io.agora.ktv.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.ExampleData;
import com.agora.data.R;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.agora.data.sync.AgoraException;
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
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

/**
 * 房间控制
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/06/01
 */
public final class RoomManager {
    private final Logger.Builder mLogger = XLog.tag("RoomManager");
    private final Logger.Builder mLoggerRTC = XLog.tag("RTC");

    private volatile static RoomManager instance;

    private final Context mContext;
    private final MainThreadDispatch mMainThreadDispatch = new MainThreadDispatch();

    /**
     * key:AgoraMember.id
     * value:AgoraMember
     */
    private final Map<String, AgoraMember> memberHashMap = new ConcurrentHashMap<>();

    private volatile AgoraRoom mRoom;
    private volatile AgoraMember mCurrentMember;

    private volatile MemberMusicModel mMusicModel;

    private RtcEngineEx mRtcEngine;

    private Integer mStreamId;

    /**
     * 唱歌人的UserId
     */
    private final List<String> singers = new ArrayList<>();

    private final IRtcEngineEventHandler mIRtcEngineEventHandler = new IRtcEngineEventHandler() {

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
        public void onUserOffline(int uid, int reason) {
            mLoggerRTC.i("onUserOffline() " + uid);
            super.onUserOffline(uid, reason);
            User user = new User();
            user.setObjectId(String.valueOf(uid));

            AgoraMember member = new AgoraMember();
            member.setId(user.getObjectId());

            mMainThreadDispatch.onMemberLeave(member);
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
        config.mAudioScenario = Constants.AUDIO_SCENARIO_CHORUS;

        try {
            mRtcEngine = (RtcEngineEx) RtcEngine.create(config);
        } catch (Exception e) {
            e.printStackTrace();
            mLoggerRTC.e("init error", e);
        }
    }

    public RtcEngineEx getRtcEngine() {
        return mRtcEngine;
    }

    /**
     * joinChannel之后只能创建5个，leaveChannel之后重置。
     *
     * @return
     */
    public Integer getStreamId() {
        if (mStreamId == null) {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.syncWithAudio = true;
            cfg.ordered = true;
            mStreamId = getRtcEngine().createDataStream(cfg);
        }
        return mStreamId;
    }

    public static RoomManager Instance(Context mContext) {
        if (instance == null) {
            synchronized (RoomManager.class) {
                if (instance == null)
                    instance = new RoomManager(mContext);
            }
        }
        return instance;
    }

    @Nullable
    public AgoraRoom getRoom() {
        return mRoom;
    }

    @Nullable
    public AgoraMember getMine() {
        return mCurrentMember;
    }

    public boolean isSinger(String userId) {
        return singers.contains(userId);
    }

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.addRoomEventCallback(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.removeRoomEventCallback(callback);
    }

    private void onMemberRoleChanged(@NonNull AgoraMember member) {
        mLogger.i("onMemberRoleChanged() called with: member = [%s]", member);
        mMainThreadDispatch.onRoleChanged(member);
    }


    public void onMusicEmpty() {
        mLogger.i("onMusicEmpty() called");
        mMusicModel = null;
        singers.clear();
        mMainThreadDispatch.onMusicEmpty();
    }

    public void onMusicChanged(MemberMusicModel model) {
        mLogger.i("onMusicChanged() called with: model = [%s]", model);

        mMusicModel = model;

        // 独唱
        if (mMusicModel.getType() == MemberMusicModel.SingType.Single) {
            singers.add(model.getUserId());
        } else if (mMusicModel.getType() == MemberMusicModel.SingType.Chorus) {
            singers.add(model.getUserId());
            singers.add(model.getUser1Id());
        }

        mMainThreadDispatch.onMusicChanged(model);
    }

    public void onMemberApplyJoinChorus(MemberMusicModel model) {
        mLogger.i("onMemberApplyJoinChorus() called with: model = [%s]", model);
        mMusicModel = model;
        mMainThreadDispatch.onMemberApplyJoinChorus(model);
    }

    public void onMemberJoinedChorus(MemberMusicModel model) {
        mLogger.i("onMemberJoinedChorus() called with: model = [%s]", model);
        mMusicModel = model;
        mMainThreadDispatch.onMemberJoinedChorus(model);
    }

    public void onMemberChorusReady(MemberMusicModel model) {
        mLogger.i("onMemberChorusReady() called with: model = [%s]", model);
        mMusicModel = model;
        mMainThreadDispatch.onMemberChorusReady(model);
    }

    @Nullable
    public MemberMusicModel getMusicModel() {
        return mMusicModel;
    }

    public boolean isMainSinger() {
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return false;
        }

        if (mMusicModel == null) {
            return false;
        }

        return ObjectsCompat.equals(mMusicModel.getUserId(), mUser.getObjectId());
    }

    public boolean isFollowSinger() {
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return false;
        }

        if (mMusicModel == null) {
            return false;
        }

        return ObjectsCompat.equals(mMusicModel.getUser1Id(), mUser.getObjectId());
    }

    public boolean isMainSinger(@NonNull AgoraMember member) {
        if (mMusicModel == null) {
            return false;
        }

        return ObjectsCompat.equals(mMusicModel.getUserId(), member.getUserId());
    }

    public Completable joinRoom(AgoraRoom room) {
        this.mRoom = room;

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return Completable.error(new NullPointerException("mUser is empty"));
        }

        mCurrentMember = new AgoraMember();
        mCurrentMember.setRoomId(mRoom);
        mCurrentMember.setId(mUser.getObjectId());
        mCurrentMember.setUserId(mUser.getObjectId());
        mCurrentMember.setUser(mUser);
        mCurrentMember.setRole(AgoraMember.Role.Listener);

        try {
            ExampleData.updateBackgroundImage(Integer.parseInt(mRoom.getMv()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Completable.complete().andThen(joinRTC().doOnSuccess(uid -> {
            Long streamId = uid & 0xffffffffL;
            mCurrentMember.setStreamId(streamId);
        }).ignoreElement()).doOnComplete(this::onJoinRoom);
    }

    private void onJoinRoom() {
        mMusicModel = null;
    }

    private SingleEmitter<Integer> emitterJoinRTC = null;

    private Single<Integer> joinRTC() {
        return Single.create(emitter -> {
            emitterJoinRTC = emitter;
            getRtcEngine().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            getRtcEngine().enableAudio();
            getRtcEngine().setParameters("{\"rtc.audio.opensl.mode\":0}");
            getRtcEngine().setParameters("{\"rtc.audio_fec\":[3,2]}");
            getRtcEngine().setParameters("{\"rtc.audio_resend\":false}");

            getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

            mLoggerRTC.i("joinRTC() called with: results = [%s]", mRoom);
            int ret = getRtcEngine().joinChannel("", mRoom.getChannelName(), null, Integer.parseInt(mCurrentMember.getId()));
            if (ret != Constants.ERR_OK) {
                mLoggerRTC.e("joinRTC() called error " + ret);
                emitter.onError(new Exception("join rtc room error " + ret));
                emitterJoinRTC = null;
            }
        });
    }

    public Completable leaveRoom() {
        mLogger.i("leaveRoom() called");

        mLoggerRTC.i("leaveChannel() called");
        getRtcEngine().leaveChannel();

        mRoom = null;
        mCurrentMember = null;
        return Completable.complete();
    }

    public Completable toggleSelfAudio(boolean isMute) {
        int mute = 0;
        if (isMute) mute = 1;
        mCurrentMember.setIsSelfMuted(mute);
        return Completable.complete();
    }

    public Completable changeRole(AgoraMember member, int role) {
        mLogger.i("changeRole:"+role+member);
        return Completable.complete();
    }
}
