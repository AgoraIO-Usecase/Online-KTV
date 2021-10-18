package io.agora.ktv.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 1.起始函数{@link MultipleMusicPlayer#prepare}。
 * 2.陪唱点击按钮"加入合唱"后，触发申请{io.agora.ktv.view.RoomActivity#joinChorus}，然后触发{MultipleMusicPlayer#onMemberApplyJoinChorus}，主唱把第一个人设置成陪唱。
 * 3.有陪唱加入后，会收到回调{@link MultipleMusicPlayer#onMemberJoinedChorus}，开始下载资源。
 * 4.{@link MultipleMusicPlayer#joinChannelEX}之后，修改状态成Ready，当所有唱歌的人都Ready后，会触发{@link MultipleMusicPlayer#onMemberChorusReady}
 */
public class MultipleMusicPlayer extends BaseMusicPlayer {

    private static final long PLAY_WAIT = 1000L;

    private final SimpleRoomEventCallback mRoomEventCallback = new SimpleRoomEventCallback() {
        @Override
        public void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {
            MultipleMusicPlayer.this.onMemberApplyJoinChorus(music);
        }

        @Override
        public void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
            MultipleMusicPlayer.this.onMemberJoinedChorus(music);
        }

        @Override
        public void onMemberChorusReady(@NonNull MemberMusicModel music) {
            MultipleMusicPlayer.this.onMemberChorusReady(music);
        }
    };

    public MultipleMusicPlayer(Context mContext, int role, IMediaPlayer mPlayer) {
        super(mContext, role, mPlayer);
        RoomManager.getInstance().getRtcEngine().setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_STANDARD);
        RoomManager.getInstance().addRoomEventCallback(mRoomEventCallback);
        this.mPlayer.adjustPlayoutVolume(80);
        this.selectAudioTrack(1);
    }

    @Override
    public void destroy() {
        super.destroy();

        leaveChannelEX();
        stopNetTestTask();
        RoomManager.getInstance().removeRoomEventCallback(mRoomEventCallback);
    }

    private boolean mRunNetTask = false;
    private Thread mNetTestThread;

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        super.onJoinChannelSuccess(channel, uid, elapsed);
    }

    private void startNetTestTask() {
        mRunNetTask = true;
        mNetTestThread = new Thread(() -> {
            while (mRunNetTask && !Thread.currentThread().isInterrupted()) {
                sendTestDelay();

                try {
                    Thread.sleep(10 * 1000L);
                } catch (InterruptedException exp) {
                    break;
                }
            }
        });
        mNetTestThread.setName("Thread-NetTest");
        mNetTestThread.start();
    }

    private void stopNetTestTask() {
        mRunNetTask = false;
        if (mNetTestThread != null) {
            mNetTestThread.interrupt();
            mNetTestThread = null;
        }
    }

    @Override
    public void prepare(@NonNull MemberMusicModel music) {
//        super.prepare(music);
//        User mUser = UserManager.Instance().getUserLiveData().getValue();
//        if (mUser == null) {
//            return;
//        }
//
//        AgoraRoom mRoom = RoomManager.Instance(mContext).getRoom();
//        if (mRoom == null) return;
//
//

//        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
//            if (music.getUserStatus() == MemberMusicModel.UserStatus.Ready) {
//                onMemberJoinedChorus(music);
//            }
//        } else if (ObjectsCompat.equals(music.getUser1Id(), mUser.getObjectId())) {
//            onMemberJoinedChorus(music);
//        } else {
//            if (!TextUtils.isEmpty(music.getUserId()) && music.getUserStatus() == MemberMusicModel.UserStatus.Ready
//                    && !TextUtils.isEmpty(music.getUser1Id()) && music.getUser1Status() == MemberMusicModel.UserStatus.Ready) {
//                onMemberChorusReady(music);
//            } else if (!TextUtils.isEmpty(music.getUser1Id()) && music.getUser1Status() == MemberMusicModel.UserStatus.Idle) {
//                onMemberJoinedChorus(music);
//            } else if (!TextUtils.isEmpty(music.getApplyUser1Id())) {
//                onMemberApplyJoinChorus(music);
//            }
//        }
    }

    private RtcConnection mRtcConnection;

    private String channelName = null;

    /**
     * 主唱
     * 分两路流
     */
    private void joinChannelEX() {

        MemberMusicModel currentMusic = RoomManager.getInstance().mCurrentMemberMusic;

        mLogger.d("joinChannelEX() called");
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        AgoraRoom mRoom = RoomManager.getInstance().getRoom();
        assert mRoom != null;
        channelName = mRoom.getId();

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = mRole;
        options.publishAudioTrack = false;
        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
        // 主唱
        if (ObjectsCompat.equals(currentMusic.getUserId(), mUser.getObjectId())) {
            options.publishMediaPlayerAudioTrack = true;
            options.enableAudioRecordingOrPlayout = false;
        } else if (ObjectsCompat.equals(currentMusic.getUser1Id(), mUser.getObjectId())) {
            options.publishMediaPlayerAudioTrack = false;
            options.enableAudioRecordingOrPlayout = false;
        }

        int uid = 0;
        if (ObjectsCompat.equals(mUser.getObjectId(), currentMusic.getUserId())) {
            if (currentMusic.getUserbgId() != null) {
                uid = currentMusic.getUserbgId().intValue();
            }
        } else if (ObjectsCompat.equals(mUser.getObjectId(), currentMusic.getUser1Id())) {
            if (currentMusic.getUser1bgId() != null) {
                uid = currentMusic.getUser1bgId().intValue();
            }
        }

        mRtcConnection = new RtcConnection();
        RoomManager.getInstance().getRtcEngine().joinChannelEx("", channelName, uid, options, null, mRtcConnection);
    }

    private void leaveChannelEX() {
        if (mRtcConnection == null) {
            return;
        }

        mLogger.d("leaveChannelEX() called");
        RoomManager.getInstance().getRtcEngine().muteAllRemoteAudioStreams(false);
        if (!TextUtils.isEmpty(channelName)) {
            RoomManager.getInstance().getRtcEngine().leaveChannelEx(channelName, mRtcConnection);
        }
        mRtcConnection = null;
    }

    @Override
    public void onMusicOpenCompleted() {
        mLogger.i("onMusicOpenCompleted() called");
        mStatus = Status.Opened;

        startDisplayLrc();
        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENCOMPLETED, mPlayer.getDuration()).sendToTarget();
    }


    private void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {
    }

    private void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
        KTVUtil.logD("onMemberJoinedChorus");
        onPrepareResource();
        ResourceManager.Instance(mContext)
                .download(music, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<MemberMusicModel>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull MemberMusicModel musicModel) {
                        if(RoomManager.getInstance().isMainSinger())
                            RoomManager.getInstance().mCurrentMemberMusic.setUserStatus(MemberMusicModel.UserStatus.Ready);
                        else
                            RoomManager.getInstance().mCurrentMemberMusic.setUser1Status(MemberMusicModel.UserStatus.Ready);
                        onResourceReady(musicModel);

                        open();

                        if (RoomManager.getInstance().isMainSinger()) {
                            joinChannelEX();
                        } else if (RoomManager.getInstance().isFollowSinger()) {
                            startNetTestTask();
                            joinChannelEX();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        ToastUtil.toastShort(mContext, R.string.ktv_lrc_load_fail);
                    }
                });
    }

    /**
     * 主唱逻辑：
     * 1.{@link MultipleMusicPlayer#sendStartPlay}通知陪唱的人，要开始了。
     * 2.陪唱人会收到{@link MultipleMusicPlayer#onReceivedStatusPlay}。
     * 3.为了保证所有人同时播放音乐，做延迟wait。
     * <p>
     * 陪唱逻辑:{@link MultipleMusicPlayer#onReceivedStatusPlay}
     */
    private void onMemberChorusReady(@NonNull MemberMusicModel music) {

        KTVUtil.logD("onMemberChorusReady");
        AgoraMember mMine = RoomManager.getInstance().getMine();
        if (mMine == null) return;

        if (RoomManager.getInstance().isMainSinger()) {
            //唱歌人，主唱，joinChannel 需要屏蔽的uid
//            RoomManager.getInstance().getRtcEngine().muteRemoteAudioStream(music.getUserbgId().intValue(), true);
//            RoomManager.getInstance().getRtcEngine().muteRemoteAudioStream(music.getUser1bgId().intValue(), true);
//            RoomManager.getInstance().getRtcEngine().muteRemoteAudioStreamEx(mMine.getStreamId().intValue(), true, mRtcConnection);
        } else if (RoomManager.getInstance().isFollowSinger()) {
            //唱歌人，陪唱人，joinChannel 需要屏蔽的uid
            RoomManager.getInstance().getRtcEngine().muteRemoteAudioStream(music.getUserbgId().intValue(), true);
//            RoomManager.getInstance().getRtcEngine().muteRemoteAudioStream(music.getUser1bgId().intValue(), true);
//            RoomManager.getInstance().getRtcEngine().muteRemoteAudioStreamEx(mMine.getStreamId().intValue(), true, mRtcConnection);
        } else {
            //观众，需要屏蔽陪唱背景音乐
//            RoomManager.getInstance().getRtcEngine().muteRemoteAudioStream(music.getUser1bgId().intValue(), true);
        }

        if (RoomManager.getInstance().isSinger()) {
            music.setFileMusic(music.getFileMusic());
            music.setFileLrc(music.getFileLrc());

            if (RoomManager.getInstance().isMainSinger()) {
                sendStartPlay();
                try {
                    synchronized (this) {
                        wait(PLAY_WAIT);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                play();
            }
        } else {
            onPrepareResource();
            onResourceReady(music);

            onMusicPlayingByListener();
            playByListener();
        }
    }

    @Override
    protected void onReceivedStatusPlay(int uid) {
        super.onReceivedStatusPlay(uid);

        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            if (mStatus == Status.Started) {
                return;
            }

            try {
                synchronized (this) {
//                    long now = System.currentTimeMillis();
//                    long remoteTs = now - offsetTS;
//                    long offset = remoteTs - time;
//                    long waitTime = PLAY_WAIT - offset;
//                    mLogger.d("onReceivedStatusPlay() called with: waitTime = [%s]", waitTime);
//                    if (waitTime > 0) {
//                        wait(waitTime);
//                    }

                    long waitTime = PLAY_WAIT - netRtt;
                    mLogger.d("onReceivedStatusPlay() called with: waitTime = [%s]", waitTime);
                    if (waitTime > 0) {
                        wait(waitTime);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            play();
        }
    }

    @Override
    protected void onReceivedStatusPause(int uid) {
        super.onReceivedStatusPause(uid);

        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {

        } else if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            pause();
        }
    }

    @Override
    protected void onReceivedSetLrcTime(int uid, long position) {
        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {

        } else if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            if (mStatus == Status.Paused) {
                resume();
            }

//            long now = System.currentTimeMillis();
//            long offsetTs = now - offsetTS - ts;
//
//            long remotePos = position + offsetTs;
//            long localPos = mPlayer.getPlayPosition();
//            long offsetPos = localPos - remotePos;
//            if (Math.abs(offsetPos) > 500) {
//                if (offsetPos > 0) {
//                    seek(remotePos + 100);
//                } else {
//                    seek(remotePos - 100);
//                }
//            }
//            mLogger.d("onReceivedSetLrcTime() called with: position = [%s], remotePos = [%s], localPos = [%s], offsetPos = [%s], offsetTs = [%s]", position, remotePos, localPos, offsetPos, offsetTs);

            long remotePos = position + (netRtt / 2);
            long localPos = mPlayer.getPlayPosition();
            long offsetPos = localPos - remotePos;
            if (Math.abs(offsetPos) > 500) {
                seek(remotePos);
            }
            mLogger.d("onReceivedSetLrcTime() called with: remotePos = [%s], localPos = [%s], offsetPos = [%s]", remotePos, localPos, offsetPos);
        } else {
            super.onReceivedSetLrcTime(uid, position);
        }
    }

    @Override
    protected void onReceivedTestDelay(int uid, long time) {
        super.onReceivedTestDelay(uid, time);

        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {
            sendReplyTestDelay(time);
        }
    }

    private long offsetTS = 0;
    private long netRtt = 0;

    @Override
    protected void onReceivedReplyTestDelay(int uid, long testDelayTime, long time) {
        super.onReceivedReplyTestDelay(uid, testDelayTime, time);
        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            long localTs = System.currentTimeMillis();
            long rtt = localTs - testDelayTime;
            offsetTS = localTs - time - rtt / 2;
            netRtt = System.currentTimeMillis() - testDelayTime;
            mLogger.d("TestDelay offsetTS= [%s], netRtt = [%s]", offsetTS, netRtt);
        }
    }

    @Override
    protected void onReceivedOriginalChanged(int uid, int mode) {
        super.onReceivedOriginalChanged(uid, mode);
        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            selectAudioTrack(mode);
        }
    }

    @Override
    public void switchRole(int role) {
        mLogger.d("switchRole() called with: role = [%s]", role);
        mRole = role;

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
        options.clientRoleType = role;
        options.publishMediaPlayerAudioTrack = false;
        if (role == Constants.CLIENT_ROLE_BROADCASTER) {
            options.publishAudioTrack = true;
        } else {
            options.publishAudioTrack = false;
        }

        RoomManager.getInstance().getRtcEngine().updateChannelMediaOptions(options);
    }

    @Override
    protected void startPublish() {
        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {
            super.startPublish();
        }
    }

    @Override
    public void togglePlay() {
        if (mStatus == Status.Started) {
            sendPause();
        } else if (mStatus == Status.Paused) {

        }

        super.togglePlay();
    }

    public void sendReplyTestDelay(long receiveTime) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "replyTestDelay");
        msg.put("testDelayTime", String.valueOf(receiveTime));
        msg.put("time", String.valueOf(System.currentTimeMillis()));
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.getInstance().getStreamId();
        int ret = RoomManager.getInstance().getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendReplyTestDelay() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    public void sendTestDelay() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "testDelay");
        msg.put("time", String.valueOf(System.currentTimeMillis()));
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.getInstance().getStreamId();
        int ret = RoomManager.getInstance().getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendTestDelay() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    public void sendStartPlay() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "setLrcTime");
        msg.put("time", 0);
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.getInstance().getStreamId();
        int ret = RoomManager.getInstance().getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendStartPlay() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    public void sendPause() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "setLrcTime");
        msg.put("time", -1);
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.getInstance().getStreamId();
        int ret = RoomManager.getInstance().getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendPause() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    @Override
    public void selectAudioTrack(int i) {
        super.selectAudioTrack(i);

        MemberMusicModel mMemberMusicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {
            sendTrackMode(i);
        }
    }

    public void sendTrackMode(int mode) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "TrackMode");
        msg.put("mode", mode);
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.getInstance().getStreamId();
        int ret = RoomManager.getInstance().getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendTrackMode() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }
}
