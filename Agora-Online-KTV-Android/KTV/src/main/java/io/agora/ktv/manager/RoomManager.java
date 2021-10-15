package io.agora.ktv.manager;

import static io.agora.ktv.bean.MemberMusicModel.UserStatus.Idle;
import static io.agora.ktv.bean.MemberMusicModel.UserStatus.Ready;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

/**
 * 房间控制
 * Manage all room related stuff, such as user join, user leave etc.
 * In the mean time, it holds a Agora RtcEngine.
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/06/01
 */
public final class RoomManager {

    ///////////////////////// —— ALL CMD ——//////////////////////////////////////////////////////////
    public static final String syncMember = "syncMember";
    public static final String requestChorus = "applyChorus";
    public static final String acceptChorus = "acceptChorus";

    private final Logger.Builder mLogger = XLog.tag("RoomManager");
    private final Logger.Builder mLoggerRTC = XLog.tag("RTC");

    private volatile static RoomManager instance;

    private final MainThreadDispatch mMainThreadDispatch = new MainThreadDispatch();

    /**
     * key:AgoraMember.id
     * value:AgoraMember
     */
    private final Map<String, AgoraMember> memberHashMap = new ConcurrentHashMap<>();

    private volatile AgoraRoom mRoom;
    private volatile AgoraMember mCurrentMember;

    // 当前演唱歌曲
    public volatile MemberMusicModel mCurrentMemberMusic;

    private RtcEngineEx mRtcEngine;

    private Integer mStreamId;

    /**
     * 唱歌人的UserId
     */
    private final Set<String> singers = new HashSet<>();

    private Thread syncMemberThread;
    private Thread syncRequestChorusThread;
    private Thread syncAcceptChorusThread;

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
                KTVUtil.logD(strMsg);
                jsonMsg = new JSONObject(strMsg);

                String cmd = null;
                try {
                    cmd = jsonMsg.getString("cmd");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (cmd == null) return;

                String remoteUserId = null;
                try {
                    remoteUserId = String.valueOf(uid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (remoteUserId == null) return;

                switch (cmd) {
                    case "setLrcTime": {
                        String musicId = "";
                        try {
                            musicId = jsonMsg.getString("lrcId");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (musicId.isEmpty()) return;


                        int state = jsonMsg.getInt("state");
                        boolean shouldPlay = state == 1;


                        // 当前不在播放 || 远端更换歌曲 《==》播放远端歌曲
                        if (shouldPlay && (mCurrentMemberMusic == null ||
                                (!musicId.equals(mCurrentMemberMusic.getMusicId()) && remoteUserId.equals(mCurrentMemberMusic.getUserId())))) {
                            MemberMusicModel temp = new MemberMusicModel(musicId);
                            temp.setUserId(remoteUserId);
                            onMusicChanged(temp);
                            // 远端切歌
                        } else if (state == 0 && remoteUserId.equals(mCurrentMemberMusic.getUserId())) {
                            onMusicEmpty();
                        }

//                        if (musicId.equals(mCurrentMemberMusic.getMusicId())) {
//
//                            long total = jsonMsg.getLong("duration");
//                            long cur = jsonMsg.getLong("time");
//                            mMainThreadDispatch.onMusicProgress(total, cur);
//                        }
                        break;
                    }
                    case RoomManager.syncMember: {
                        String userId = jsonMsg.getString("userId");
                        int role = jsonMsg.getInt("role");

                        AgoraMember tempMember = memberHashMap.get(userId);

                        // Add member
                        if (tempMember == null) {
                            String avatar = jsonMsg.getString("avatar");
                            AgoraMember member = new AgoraMember();
                            User user = new User();
                            user.setAvatar(avatar);
                            user.setObjectId(userId);

                            member.setId(user.getObjectId());
                            member.setUser(user);
                            member.setRole(AgoraMember.Role.parse(role));
                            memberHashMap.put(userId, member);
                            mMainThreadDispatch.onMemberJoin(member);
                        } else if (role == AgoraMember.Role.Listener.getValue()) {
                            memberHashMap.remove(userId);
                            mMainThreadDispatch.onMemberLeave(tempMember);
                        }
                        break;
                    }

                    /*
                     * 收到 "请求合唱" 的消息
                     *
                     * 需要当前是 主唱端
                     *
                     * musicStatus 0: 设置当前用户为陪唱，开始准备数据
                     * musicStatus 1: 开始播放
                     *
                     */
                    case RoomManager.requestChorus: {

                        if (isMainSinger()) {

                            String userId = jsonMsg.getString("userId");

                            if (mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Chorus && mCurrentMemberMusic.getUser1Id() == null) {
                                KTVUtil.logD("requestChorus---- case1");
                                // Mark as accepted
                                singers.add(userId);
                                mCurrentMemberMusic.setUser1Id(userId);

                                // start prepare and countdown will terminate
                                mMainThreadDispatch.onMemberJoinedChorus(mCurrentMemberMusic);

                                startSyncAcceptChorus();
                            } else if (userId.equals(mCurrentMemberMusic.getUser1Id()) && jsonMsg.getInt("musicStatus") == 1
                                    && (mCurrentMemberMusic.getUserStatus() != Ready || mCurrentMemberMusic.getUser1Status() != Ready)) {
                                KTVUtil.logD("requestChorus---- case2");
                                mCurrentMemberMusic.setUser1Status(Ready);

                                if(mCurrentMemberMusic.getUserStatus() == Ready) {
                                    stopSyncAcceptChorus();
                                    sendSyncAcceptChorusMsg();
                                    mMainThreadDispatch.onMemberChorusReady(mCurrentMemberMusic);
                                }
                            }
                        }
                        break;
                    }

                    /*
                     * 收到 "接受请求合唱" 的消息
                     *
                     * 需要当前是 陪唱端
                     *
                     * musicStatus 0: 设置当前用户为陪唱，开始准备数据
                     * musicStatus 1: 开始播放
                     *
                     */
                    case RoomManager.acceptChorus: {

                        String userId = jsonMsg.getString("userId");
                        String acceptId = jsonMsg.getString("acceptedUid");

                        if(acceptId.equals(UserManager.Instance().getUser().getObjectId())) {

                            if (mCurrentMemberMusic != null && mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Chorus
                                    && mCurrentMemberMusic.getUser1Id() == null && ObjectsCompat.equals(mCurrentMemberMusic.getUserId(), remoteUserId)) {

                            KTVUtil.logD("acceptChorus---- case1");
                                // Mark as accepted
                                mCurrentMemberMusic.setUser1Id(UserManager.Instance().getUser().getObjectId());
                                // Start Prepare
                                mMainThreadDispatch.onMemberJoinedChorus(mCurrentMemberMusic);

                                // 确认是陪唱 && 发消息的是主唱
                            } else if (isFollowSinger() && userId.equals(mCurrentMemberMusic.getUserId())) {
                                if (jsonMsg.getInt("musicStatus") == 1) {
                                    KTVUtil.logD("acceptChorus---- case2");
                                    mCurrentMemberMusic.setUserStatus(Ready);
                                    stopSyncRequestChorus();
                                    mMainThreadDispatch.onMemberChorusReady(mCurrentMemberMusic);
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (JSONException exp) {
                exp.printStackTrace();
            }
        }
    };

    private RoomManager(Context mContext) {
        iniRTC(mContext);
    }

    private void iniRTC(Context mContext) {
        String APP_ID = mContext.getString(R.string.app_id);
        if (TextUtils.isEmpty(APP_ID)) {
            ToastUtil.toastShort(mContext, "please check \"strings_config.xml\"");
            throw new NullPointerException("please check \"strings_config.xml\"");
        }

        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = mContext;
        config.mAppId = APP_ID;
        config.mEventHandler = mIRtcEngineEventHandler;
        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        config.mAudioScenario = Constants.AUDIO_SCENARIO_CHORUS;
        config.getLogConfig().level = Constants.LogLevel.getValue(Constants.LogLevel.LOG_LEVEL_NONE);

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
     */
    public Integer getStreamId() {
        if (mStreamId == null) {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.syncWithAudio = false;
            cfg.ordered = false;
            mStreamId = getRtcEngine().createDataStream(cfg);
        }
        return mStreamId;
    }

    public static RoomManager getInstance(Context mContext) {
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
        mCurrentMemberMusic = null;
        singers.clear();
        mMainThreadDispatch.onMusicEmpty();
    }

    public void onMusicChanged(MemberMusicModel model) {
        mLogger.i("onMusicChanged() called with: model = [%s]", model);

        mCurrentMemberMusic = model;

        // 独唱
        if (mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Single) {
            singers.add(model.getUserId());
        } else if (mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Chorus) {
            singers.add(model.getUserId());
            singers.add(model.getUser1Id());
        }

        mMainThreadDispatch.onMusicChanged(model);
    }

    public void onMemberApplyJoinChorus(MemberMusicModel model) {
        mLogger.i("onMemberApplyJoinChorus() called with: model = [%s]", model);
        mCurrentMemberMusic = model;
        mMainThreadDispatch.onMemberApplyJoinChorus(model);
    }

    public void onMemberJoinedChorus(MemberMusicModel model) {
        mLogger.i("onMemberJoinedChorus() called with: model = [%s]", model);
        mCurrentMemberMusic = model;
        mMainThreadDispatch.onMemberJoinedChorus(model);
    }

    public void onMemberChorusReady(MemberMusicModel model) {
        mLogger.i("onMemberChorusReady() called with: model = [%s]", model);
        mCurrentMemberMusic = model;
        mMainThreadDispatch.onMemberChorusReady(model);
    }

    public boolean isSinger() {
        return isMainSinger() || isFollowSinger();
    }

    public boolean isMainSinger() {
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null || mCurrentMemberMusic == null) {
            return false;
        }
        return ObjectsCompat.equals(mCurrentMemberMusic.getUserId(), mUser.getObjectId());
    }

    public boolean isFollowSinger() {
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null || mCurrentMemberMusic == null) {
            return false;
        }

        return ObjectsCompat.equals(mCurrentMemberMusic.getUser1Id(), mUser.getObjectId());
    }

    public boolean isMainSinger(@NonNull AgoraMember member) {
        if (mCurrentMemberMusic == null) {
            return false;
        }

        return ObjectsCompat.equals(mCurrentMemberMusic.getUserId(), member.getUserId());
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
            ExampleData.updateCoverImage(Integer.parseInt(mRoom.getMv()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Completable.complete().andThen(joinRTC().doOnSuccess(uid -> {
            Long streamId = uid & 0xffffffffL;
            mCurrentMember.setStreamId(streamId);
        }).ignoreElement()).doOnComplete(this::onJoinRoom);
    }

    private void onJoinRoom() {
        mCurrentMemberMusic = null;
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
        stopSyncMember();
        stopSyncAcceptChorus();
        stopSyncRequestChorus();
        mMainThreadDispatch.destroy();
        mLogger.i("leaveRoom() called");

        mLoggerRTC.i("leaveChannel() called");
        getRtcEngine().leaveChannel();

        mRoom = null;
        mCurrentMember = null;
        instance = null;
        return Completable.complete();
    }

    public Completable toggleSelfAudio(boolean mute) {
        mCurrentMember.setIsSelfMuted(mute ? 1 : 0);

        if (getRtcEngine() != null) {
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishAudioTrack = !mute;
            getRtcEngine().updateChannelMediaOptions(options);
        }
        return Completable.complete();
    }

    public void changeCurrentRole(int role) {
        getRtcEngine().setClientRole(role);
    }

    //<editor-fold desc="Sync Stuff">

    ////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////———— SYNC MEMBER ————//////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    public void startSyncMember() {
        if (syncMemberThread == null) {
            syncMemberThread = new Thread(() -> {
                while (!syncMemberThread.isInterrupted() && getRtcEngine() != null) {
                    sendSyncMemberMsg();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            syncMemberThread.start();
        }
    }

    /**
     * 上麦主播每秒广播一次自己状态
     */
    private void sendSyncMemberMsg() {
        if (getRtcEngine() == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", RoomManager.syncMember);
        msg.put("userId", mCurrentMember.getUserId());
        msg.put("role", mCurrentMember.getRole().getValue());
        msg.put("avatar", mCurrentMember.getUser().getAvatar());

        sendStreamMsg(msg);
    }

    public void stopSyncMember() {
        if (syncMemberThread != null) {
            syncMemberThread.interrupt();
        }
        mCurrentMember.setRole(AgoraMember.Role.Listener);
        sendSyncMemberMsg();
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// ———— APPLY CHORUS ———— ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    public void startSyncRequestChorus() {
        if (syncRequestChorusThread == null) {
            syncRequestChorusThread = new Thread(() -> {
                while (!syncRequestChorusThread.isInterrupted() && getRtcEngine() != null) {
                    sendSyncRequestChorusMsg();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            syncRequestChorusThread.start();
        }
    }

    /**
     * 点击接受合唱按钮之后, 每一秒发送一次下面这个申请合唱的请求
     * 如果成功被主唱接受, 则开始下载歌词歌曲, 直到下载完成 musicStatus更新成1, 并继续发送, 直到收到主唱发送musicStatus为1的acceptChorus的消息
     * <p>
     * {"userId":2222,"msgId":1,"cmd":"applyChorus","musicId":"6246262727281640","musicStatus":0}
     */
    private void sendSyncRequestChorusMsg() {
        if (getRtcEngine() == null || mCurrentMemberMusic == null || mCurrentMember == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", RoomManager.requestChorus);
        msg.put("userId", mCurrentMember.getUserId());
        msg.put("musicId", mCurrentMemberMusic.getMusicId());
        msg.put("musicStatus", mCurrentMemberMusic.getUser1Status() == Ready ? 1 : 0);

        sendStreamMsg(msg);
    }

    public void stopSyncRequestChorus() {
        if (syncAcceptChorusThread != null) {
            syncAcceptChorusThread.interrupt();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// ———— ACCEPT CHORUS ———— //////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    public void startSyncAcceptChorus() {
        if (syncAcceptChorusThread == null) {
            syncAcceptChorusThread = new Thread(() -> {
                while (!syncAcceptChorusThread.isInterrupted() && getRtcEngine() != null) {
                    sendSyncAcceptChorusMsg();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            syncAcceptChorusThread.start();
        }
    }

    /**
     * 主唱端收到的第一个applyChorus的消息就自动接收,每一秒发送acceptChorus消息,并开始下载歌词歌曲
     * 下载完成musicStatus更新成1, 并继续发送,直到收到辅唱发送musicStatus为1的applyChorus的消息
     * {"userId":1234,"msgId":1,"cmd":"acceptChorus","acceptedUid":2222,"musicStatus":0}
     */
    private void sendSyncAcceptChorusMsg() {
        if (getRtcEngine() == null || mCurrentMemberMusic == null) return;

        boolean ready = mCurrentMemberMusic.getUserStatus() == Ready && mCurrentMemberMusic.getUser1Status() == Ready;

        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", RoomManager.acceptChorus);
        msg.put("userId", mCurrentMember.getUserId());
        msg.put("musicId", mCurrentMemberMusic.getMusicId());
        msg.put("acceptedUid", mCurrentMemberMusic.getUser1Id());
        msg.put("musicStatus", ready ? 1 : 0);
        sendStreamMsg(msg);
    }

    public void stopSyncAcceptChorus() {
        if (syncAcceptChorusThread != null) {
            syncAcceptChorusThread.interrupt();
        }
    }


    public void sendStreamMsg(Map<String, Object> mapMsg) {
        String msg = new JSONObject(mapMsg).toString();
        int res = getRtcEngine().sendStreamMessage(getStreamId(), msg.getBytes());
        if (res < 0) mLogger.e("sendStreamMsg() called returned: ret = [%s], msg = %s", res, msg);
    }

    //</editor-fold>
}
