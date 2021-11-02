package io.agora.ktv.manager;

import static io.agora.ktv.bean.MemberMusicModel.UserStatus.Ready;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.agora.ktv.R;
import io.agora.ktv.repo.DataRepositoryImpl;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.base.internal.ContextUtils;
import io.agora.baselibrary.util.KTVUtil;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

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
    public static final String syncMusic = "setLrcTime";
    public static final String requestChorus = "applyChorus";
    public static final String acceptChorus = "acceptChorus";
    public static final String countdown = "countdown";

    private volatile static RoomManager instance;

    private final MainThreadDispatch mMainThreadDispatch = new MainThreadDispatch();

    /**
     * key:AgoraMember.id
     * value:AgoraMember
     */
    private final Map<Integer, AgoraMember> memberHashMap = new ConcurrentHashMap<>();

    private volatile AgoraRoom mRoom;
    private volatile AgoraMember mCurrentMember;

    // 当前演唱歌曲
    public volatile MemberMusicModel mCurrentMemberMusic;

    private RtcEngineEx mRtcEngine;

    private Integer mStreamId;

    private @Nullable Thread syncMemberThread;
    private @Nullable Thread syncRequestChorusThread;
    private @Nullable Thread syncAcceptChorusThread;

    private final IRtcEngineEventHandler mIRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            User user = new User();
            user.setUserId(uid);

            AgoraMember member = new AgoraMember();
            member.setId(user.getUserId());

            mMainThreadDispatch.onMemberLeave(member);

            if (mCurrentMemberMusic != null && mCurrentMemberMusic.getUserId() == uid)
                onMusicEmpty();
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {

            JSONObject jsonMsg;
            try {
                String strMsg = new String(data);
//                KTVUtil.logD("RECV <<< " +strMsg);
                jsonMsg = new JSONObject(strMsg);

                /*
                    Check cmd
                 */
                String cmd = null;
                try {
                    cmd = jsonMsg.getString("cmd");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (cmd == null) return;

                /*
                    Get remote user id by uid
                 */
//                String remoteUserId = null;
//                try {
//                    remoteUserId = String.valueOf(uid);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                if (remoteUserId == null) return;

                /*
                    Check cmd
                 */
                switch (cmd) {
                    case RoomManager.countdown: {
                        int time = jsonMsg.getInt("time");
                        String musicId = jsonMsg.getString("musicId");

                        mMainThreadDispatch.onReceivedCountdown(uid, time, musicId);
                        break;
                    }
                    case RoomManager.syncMusic: {
                        String musicId = "";
                        try {
                            musicId = jsonMsg.getString("lrcId");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (musicId.isEmpty()) return;


                        int state = jsonMsg.getInt("state");
                        boolean shouldPlay = (state == 1) || (state == 2);

                        // 当前不在播放 ||在countdown|| 远端更换歌曲 《==》播放远端歌曲
                        if (shouldPlay && (mCurrentMemberMusic == null || (mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Chorus && mCurrentMemberMusic.getUser1Id() == 0) ||
                                (uid == mCurrentMemberMusic.getUserId() && !musicId.equals(mCurrentMemberMusic.getMusicId())))) {
                            KTVUtil.logD("syncMusic ---- step1");
                            MemberMusicModel temp = new MemberMusicModel(musicId);
                            temp.setUserId(uid);
                            onMusicChanged(temp);
                            // 远端切歌
                        } else if (state == 0 && uid == mCurrentMemberMusic.getUserId()) {
                            KTVUtil.logD("syncMusic ---- step2");
                            onMusicEmpty();
                        }

                        if (mCurrentMemberMusic != null && mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Chorus) {
                            int playerId = -1;
                            try {
                                playerId = jsonMsg.getInt("playerUid");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (playerId != -1) {
                                mRtcEngine.muteRemoteAudioStream(playerId, true);
                            }
                        }

                        break;
                    }
                    case RoomManager.syncMember: {
                        int role = jsonMsg.getInt("role");

                        AgoraMember tempMember = memberHashMap.get(uid);

                        // Add member
                        if (tempMember == null) {
                            if (role == 2) {
                                String avatar = jsonMsg.getString("avatar");
                                AgoraMember member = new AgoraMember();
                                User user = new User();
                                user.setAvatar(avatar);
                                user.setUserId(uid);

                                member.setUser(user);
                                memberHashMap.put(uid, member);
                                mMainThreadDispatch.onMemberJoin(member);
                            }
                        } else if (role == 1) {
                            memberHashMap.remove(uid);
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

                            // 当前为合唱歌曲 && 目前还没有辅唱
                            if (mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Chorus && mCurrentMemberMusic.getUser1Id() == 0) {
                                KTVUtil.logD("requestChorus---- case1");
                                // Mark as accepted
                                mCurrentMemberMusic.setUser1Id(uid);

                                // start prepare and countdown will terminate
                                mMainThreadDispatch.onMemberJoinedChorus(mCurrentMemberMusic);

                                startSyncAcceptChorus();
                                // 辅唱端 汇报自己准备完毕《==》id为辅唱id && status == 1 && 当前不 ready
                            } else if (uid == mCurrentMemberMusic.getUser1Id() && jsonMsg.getInt("musicStatus") == 1
                                    && (mCurrentMemberMusic.getUserStatus() != Ready || mCurrentMemberMusic.getUser1Status() != Ready)) {
                                KTVUtil.logD("requestChorus---- case2");
                                mCurrentMemberMusic.setUser1Status(Ready);

                                // 两端都 read《==》开始播放
                                if (mCurrentMemberMusic.getUserStatus() == Ready) {
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

//                        String userId = jsonMsg.getString("userId");
                        int acceptId = jsonMsg.getInt("acceptedUid");

                        if (acceptId == UserManager.getInstance().mUser.getUserId()) {

                            if (mCurrentMemberMusic != null && mCurrentMemberMusic.getType() == MemberMusicModel.SingType.Chorus
                                    && uid == mCurrentMemberMusic.getUserId()) {

                                if (mCurrentMemberMusic.getUser1Id() == 0) {
                                    KTVUtil.logD("acceptChorus---- case1");
                                    // Mark as accepted
                                    mCurrentMemberMusic.setUser1Id(UserManager.getInstance().mUser.getUserId());
                                    // Start Prepare
                                    mMainThreadDispatch.onMemberJoinedChorus(mCurrentMemberMusic);
                                    // 确认是陪唱 && 发消息的是主唱
                                } else if (isFollowSinger() && jsonMsg.getInt("musicStatus") == 1) {

                                    if (mCurrentMemberMusic.getUserStatus() != Ready) {
                                        KTVUtil.logD("acceptChorus---- case2");
                                        mCurrentMemberMusic.setUserStatus(Ready);
                                        stopSyncRequestChorus();
                                        mMainThreadDispatch.onMemberChorusReady(mCurrentMemberMusic);
                                    }
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

    //<editor-fold desc="INIT">
    private RoomManager() {
    }

    public static RoomManager getInstance() {
        if (instance == null) {
            synchronized (RoomManager.class) {
                if (instance == null)
                    instance = new RoomManager();
            }
        }
        return instance;
    }

    public Completable initEngine(Context mContext) {
        return Completable.create(emitter -> {
            String APP_ID = mContext.getString(R.string.app_id);
            if (TextUtils.isEmpty(APP_ID)) {
                emitter.onError(new NullPointerException("APP_ID is empty, please check \"strings_config.xml\""));
            }

            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = mContext;
            config.mAppId = APP_ID;
            config.mEventHandler = mIRtcEngineEventHandler;
            config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            config.mAudioScenario = Constants.AUDIO_SCENARIO_CHORUS;

            mRtcEngine = (RtcEngineEx) RtcEngine.create(config);
            emitter.onComplete();
        }).observeOn(Schedulers.io());
    }


    public Completable joinRoom(AgoraRoom room) {
        return Completable.create(emitter -> {
            KTVUtil.logD("joinRoom ---" + Thread.currentThread().getName());
            RoomManager.this.mRoom = room;

            User mUser = UserManager.getInstance().mUser;
            if (mUser == null) {
                emitter.onError(new NullPointerException("mUser is empty"));
            } else {
                mCurrentMember = new AgoraMember();
                mCurrentMember.setRoomId(mRoom);
                mCurrentMember.setId(mUser.getUserId());
                mCurrentMember.setUserId(mUser.getUserId());
                mCurrentMember.setUser(mUser);
                mCurrentMember.setRole(Constants.CLIENT_ROLE_AUDIENCE);

                try {
                    updateCoverImage(Integer.parseInt(mRoom.getMv()));
                    onJoinRoom();
                    emitter.onComplete();
                } catch (NumberFormatException e) {
                    emitter.onError(e);
                }
            }
        });
    }

    private void onJoinRoom() {
        KTVUtil.logD("Thread onJoinRoom:" + Thread.currentThread().getName());
        mCurrentMemberMusic = null;
    }

    public Completable joinRTC() {
//        KTVUtil.logD("Thread joinRTC1:" + Thread.currentThread().getName());
//        if(mRtcEngine == null) return Completable.complete();
        return Completable.create(emitter -> {
            KTVUtil.logD("Thread joinRTC:" + Thread.currentThread().getName());
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.enableAudio();
            mRtcEngine.setParameters("{\"rtc.audio.opensl.mode\":0}");
            mRtcEngine.setParameters("{\"rtc.audio_fec\":[3,2]}");
            mRtcEngine.setParameters("{\"rtc.audio_resend\":false}");

            // 默认不允许说话
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishAudioTrack = false;
            mRtcEngine.updateChannelMediaOptions(options);

            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

            int res = mRtcEngine.joinChannel("", mRoom.getChannelName(), null, UserManager.getInstance().mUser.getUserId());
            KTVUtil.logD("res:" + res);
            if (res != Constants.ERR_OK) {
                KTVUtil.logD("joinRTC() called error " + res);
                emitter.onError(new Exception("join rtc room error " + res));
            } else {
                KTVUtil.logD("COMPLETE");
                emitter.onComplete();
            }
        });
    }
    //</editor-fold>

    public RtcEngineEx getRtcEngine() {
        return mRtcEngine;
    }

    public Integer getStreamId() {
        if (mStreamId == null) {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.syncWithAudio = false;
            cfg.ordered = false;
            mStreamId = getRtcEngine().createDataStream(cfg);
        }
        return mStreamId;
    }

    @Nullable
    public AgoraRoom getRoom() {
        return mRoom;
    }

    @Nullable
    public AgoraMember getMine() {
        return mCurrentMember;
    }

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.addRoomEventCallback(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.removeRoomEventCallback(callback);
    }

    public void onMusicEmpty() {
        KTVUtil.logD("onMusicEmpty() called");
        if (mCurrentMemberMusic != null) {
            sendSyncLrc(mCurrentMemberMusic.getMusicId(), 0, 0, 0);
            mCurrentMemberMusic = null;
        }
        memberHashMap.clear();
        mMainThreadDispatch.onMusicEmpty();
        stopSyncRequestChorus();
        stopSyncAcceptChorus();
    }

    @SuppressLint("CheckResult")
    public void onMusicChanged(MemberMusicModel model) {
        KTVUtil.logD("onMusicChanged() called with: model = " + model);

        mCurrentMemberMusic = model;

        DataRepositoryImpl.getInstance().getMusic(model.getMusicId()).subscribe(musicModel -> {
            mCurrentMemberMusic.setPropertiesWithMusic(musicModel);

            mMainThreadDispatch.onMusicChanged(model);
        });
    }

    public boolean isSinger() {
        return isMainSinger() || isFollowSinger();
    }

    public boolean isMainSinger() {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null || mCurrentMemberMusic == null || mCurrentMemberMusic.getUserId() == 0) {
            return false;
        }
        return mCurrentMemberMusic.getUserId() == mUser.getUserId();
    }

    public boolean isFollowSinger() {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null || mCurrentMemberMusic == null || mCurrentMemberMusic.getUser1Id() == 0) {
            return false;
        }

        return mCurrentMemberMusic.getUser1Id() == mUser.getUserId();
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
        if (mCurrentMember != null)
            mCurrentMember.setRole(role);
        if (mRtcEngine != null)
            getRtcEngine().setClientRole(role);
    }

    /**
     * 歌房封面图
     */
    private final MutableLiveData<Integer> mvImage = new MutableLiveData<>(0);

    public LiveData<Integer> getMvImage() {
        return mvImage;
    }

    public void updateCoverImage(int index) {
        mvImage.postValue(index);
    }

    //<editor-fold desc="Destroy Stuff">
    public void leaveRoom() {
        stopSyncMember();
        stopSyncAcceptChorus();
        stopSyncRequestChorus();

        if (mCurrentMemberMusic != null) {
            sendSyncLrc(mCurrentMemberMusic.getMusicId(), 0, 0, 0);
        }

        mMainThreadDispatch.destroy();
        KTVUtil.logD("leaveRoom() called");

        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
        RtcEngine.destroy();

        mRtcEngine = null;
        mRoom = null;
        mCurrentMember = null;
        ContextUtils.uninitialize();

        instance = null;
    }
    //</editor-fold>

    //<editor-fold desc="Sync Stuff">

    ////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////———— SYNC MEMBER ————//////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    public void startSyncMember() {
        if (syncMemberThread != null) syncMemberThread.interrupt();

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

    /**
     * 上麦主播每秒广播一次自己状态
     */
    private void sendSyncMemberMsg() {
        if (getRtcEngine() == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", RoomManager.syncMember);
        msg.put("userId", mCurrentMember.getUserId());
        msg.put("role", mCurrentMember.getRole() == Constants.CLIENT_ROLE_BROADCASTER ? 2 : 1);
        msg.put("avatar", mCurrentMember.getUser().getAvatar());

        sendStreamMsg(msg);
    }

    public void stopSyncMember() {
        if (syncMemberThread != null) {
            syncMemberThread.interrupt();
        }
        if (mCurrentMember != null) {
            mCurrentMember.setRole(Constants.CLIENT_ROLE_AUDIENCE);
            sendSyncMemberMsg();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// ———— APPLY CHORUS ———— ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    public void startSyncRequestChorus() {
        if (syncRequestChorusThread != null) syncRequestChorusThread.interrupt();

        syncRequestChorusThread = new Thread(() -> {
            while (!syncRequestChorusThread.isInterrupted() && getRtcEngine() != null && mCurrentMemberMusic != null) {
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

    /**
     * 点击接受合唱按钮之后, 每一秒发送一次下面这个申请合唱的请求
     * 如果成功被主唱接受, 则开始下载歌词歌曲, 直到下载完成 musicStatus更新成1, 并继续发送, 直到收到主唱发送musicStatus为1的acceptChorus的消息
     * <p>
     * {"userId":2222,"msgId":1,"cmd":"applyChorus","musicId":"6246262727281640","musicStatus":0}
     */
    private void sendSyncRequestChorusMsg() {
        if (mCurrentMemberMusic == null || mCurrentMember == null) return;

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
        if (syncAcceptChorusThread != null) syncAcceptChorusThread.interrupt();

        syncAcceptChorusThread = new Thread(() -> {
            while (!syncAcceptChorusThread.isInterrupted() && getRtcEngine() != null && mCurrentMemberMusic != null) {
                KTVUtil.logD("syncAcceptChorusThread");
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

    /**
     * 主唱端收到的第一个applyChorus的消息就自动接收,每一秒发送acceptChorus消息,并开始下载歌词歌曲
     * 下载完成musicStatus更新成1, 并继续发送,直到收到辅唱发送musicStatus为1的applyChorus的消息
     * {"userId":1234,"msgId":1,"cmd":"acceptChorus","acceptedUid":2222,"musicStatus":0}
     */
    private void sendSyncAcceptChorusMsg() {
        if (mCurrentMemberMusic == null) return;

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
        if (mRtcEngine != null) {
            String msg = new JSONObject(mapMsg).toString();
            KTVUtil.logD("SEND >>> " + msg);
            int res = getRtcEngine().sendStreamMessage(getStreamId(), msg.getBytes());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// ———— COUNTDOWN ———— //////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    public void sendSyncCountdown(int time) {
        if (mCurrentMemberMusic == null) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "countdown");
        msg.put("time", time);
        msg.put("userId", mCurrentMemberMusic.getUserId());
        msg.put("musicId", RoomManager.getInstance().mCurrentMemberMusic.getMusicId());
        sendStreamMsg(msg);
    }

    /**
     * setLrcTime 主唱每次postition changed的时候广播, 切歌或者自然播完的时候 更新 state = 0, 暂停歌曲 state = 2
     * {"time":0,"userId":1234,"ts":"1632655517979","lrcId":"6246262727281640","duration":270706,"cmd":"setLrcTime","msgId":5,"playerUid":12341234,"state":1}
     */
    public void sendSyncLrc(String musicId, long duration, long time, int state) {

        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "setLrcTime");
        msg.put("lrcId", musicId);
        msg.put("duration", duration);
        msg.put("time", time);//ms
        msg.put("state", state);
        msg.put("userId", UserManager.getInstance().mUser.getUserId());
        if (mCurrentMemberMusic != null)
            msg.put("playerUid", mCurrentMemberMusic.getUserPlayerId());
        RoomManager.getInstance().sendStreamMsg(msg);
    }
    //</editor-fold>
}
