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
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.json.JSONException;
import org.json.JSONObject;

import io.agora.ktv.bean.MemberMusicModel;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.RtcEngineConfig;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

/**
 * 房间控制
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/06/01
 */
@SuppressWarnings("ALL")
public final class RoomManager {
    private final Logger.Builder mLogger = XLog.tag("RoomManager");
    private final Logger.Builder mLoggerRTC = XLog.tag("RTC");

    private volatile static RoomManager instance;

    private Context mContext;
    private final MainThreadDispatch mMainThreadDispatch = new MainThreadDispatch();

//    private final Map<String, AgoraMember> memberHashMap = new ConcurrentHashMap<>();

    private volatile static AgoraRoom mRoom;
    private volatile static AgoraMember owner;
    private volatile static AgoraMember mMine;

    private volatile MemberMusicModel mMusicModel;

    private RtcEngine mRtcEngine;

    /**
     * 唱歌人的UserId
     */
//    private final List<String> singers = new ArrayList<>();

    private IRtcEngineEventHandler mIRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onError(int err) {
            super.onError(err);
            mLogger.e("onError() called with: err = [%s]", err);
        }

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
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            mLoggerRTC.i("onLeaveChannel() called with: stats = [%s]", stats);
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            JSONObject jsonMsg;
            try {
                String strMsg = new String(data);
                jsonMsg = new JSONObject(strMsg);

                String cmd = jsonMsg.getString("cmd");

                mLoggerRTC.d(jsonMsg);
                if (cmd == null) return;

                if (cmd.equals("setLrcTime") ) {
                    String musicId = jsonMsg.getString("lrcId");
                    if (musicId.isEmpty()) return;
                    String remoteUserId = null;
                    try {
                        remoteUserId = String.valueOf(uid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (remoteUserId == null) return;
                    boolean shouldPlay = jsonMsg.getInt("state") == 1;
                    // 当前不在播放 || 远端更换歌曲 《==》播放远端歌曲
                    if((mMusicModel == null && shouldPlay) ||
                            (mMusicModel != null && !mMusicModel.getMusicId().equals(musicId) && mMusicModel.getUserId().equals(remoteUserId))
                    && shouldPlay) {
                        mMusicModel = new MemberMusicModel(musicId);
                        mMusicModel.setId(musicId);
                        mMusicModel.setUserId(remoteUserId);
                        onMusicChanged(mMusicModel);
                        // 远端切歌
                    }else if(jsonMsg.getInt("state") == 0 && mMusicModel.getUserId().equals(remoteUserId)){
                        onMusicEmpty();
                    }
                } else if (cmd.equals("syncMember")) {
                    AgoraMember member = new AgoraMember();
                    User user = new User();
                    user.setAvatar(jsonMsg.getString("avatar"));
                    user.setObjectId(jsonMsg.getString("userId"));

                    member.setId(user.getObjectId());
                    member.setUser(user);
                    member.setRole(AgoraMember.Role.parse(jsonMsg.getInt("role")));
                    mMainThreadDispatch.onMemberJoin(member);
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
//        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
//        if (Config.isLeanCloud()) {
//            config.mAreaCode = RtcEngineConfig.AreaCode.AREA_CODE_CN;
//        } else {
//            config.mAreaCode = RtcEngineConfig.AreaCode.AREA_CODE_GLOB;
//        }

        try {
            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
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

//    public boolean isSinger(String userId) {
//        return singers.contains(userId);
//    }

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

    private void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
        mLogger.i("onAudioStatusChanged() called with: isMine = [%s], member = [%s]", isMine, member);
        mMainThreadDispatch.onAudioStatusChanged(isMine, member);
    }

    public void onMusicEmpty() {
        mLogger.i("onMusicEmpty() called");
        mMainThreadDispatch.onMusicEmpty();
        mMusicModel = null;
    }

    public void onMusicChanged(MemberMusicModel model) {
        mLogger.i("onMusicChanged() called with: model = [%s]", model);
        mMusicModel = model;
        mMainThreadDispatch.onMusicChanged(mMusicModel);
    }

    private final static Object musicObject = new Object();

    @Nullable
    public MemberMusicModel getMusicModel() {
        return mMusicModel;
    }

    public Completable joinRoom(AgoraRoom room) {
        mRoom = room;

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return Completable.error(new NullPointerException("mUser is empty"));
        }

        mMine = new AgoraMember();
        mMine.setRoomId(mRoom);
        mMine.setId(mUser.getObjectId());
        mMine.setUserId(mUser.getObjectId());
        mMine.setUser(mUser);
        mMine.setRole(AgoraMember.Role.Listener);

        try {
            ExampleData.updateBackgroundImage(Integer.parseInt(mRoom.getMv()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Completable.complete().andThen(joinRTC().doOnSuccess(uid -> {
            Long streamId = uid & 0xffffffffL;
            mMine.setStreamId(streamId);
        }).ignoreElement())
                .doOnComplete(() -> onJoinRoom());
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
            if (ObjectsCompat.equals(mMine, owner)) {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            } else if (mMine.getRole() == AgoraMember.Role.Speaker) {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            } else {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            }

            mLoggerRTC.i("joinRTC() called with: results = [%s]", mRoom);
            int ret = getRtcEngine().joinChannel("", mRoom.getChannelName(), null, Integer.parseInt(mMine.getId()));
            if (ret != Constants.ERR_OK) {
                mLoggerRTC.e("joinRTC() called error " + ret);
                emitter.onError(new Exception("join rtc room error " + ret));
                emitterJoinRTC = null;
            }
        });
    }

    public Completable leaveRoom() {
        mLogger.i("leaveRoom() called");
        if (mRoom == null) {
            return Completable.complete();
        }

        mLoggerRTC.i("leaveChannel() called");
        getRtcEngine().leaveChannel();

        mRoom = null;
        mMine = null;
        owner = null;
        return Completable.complete();
    }

    public Completable toggleSelfAudio(boolean isMute) {
        int mute = 0;
        if (isMute) mute = 1;
        mMine.setIsSelfMuted(mute);
        return Completable.complete();
    }
}
