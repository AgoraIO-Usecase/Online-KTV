package io.agora.ktv.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.agora.ktv.bean.MemberMusicModel;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;

public abstract class BaseMusicPlayer extends IRtcEngineEventHandler implements IMediaPlayerObserver {
    protected final Logger.Builder mLogger = XLog.tag("MusicPlayer");

    protected final Context mContext;
    protected int mRole = Constants.CLIENT_ROLE_BROADCASTER;

    //主唱同步歌词给其他人
    private boolean mStopSyncLrc = true;
    private Thread mSyncLrcThread;

    //歌词实时刷新
    protected boolean mStopDisplayLrc = true;
    private Thread mDisplayThread;

    protected IMediaPlayer mPlayer;

    private static volatile long mRecvedPlayPosition = 0;//播放器播放position，ms
    private static volatile Long mLastRecvPlayPosTime = null;

    protected static volatile MemberMusicModel mMusicModel;

    private Callback mCallback;

    protected static final int ACTION_UPDATE_TIME = 100;
    protected static final int ACTION_ONMUSIC_OPENING = ACTION_UPDATE_TIME + 1;
    protected static final int ACTION_ON_MUSIC_OPENCOMPLETED = ACTION_ONMUSIC_OPENING + 1;
    protected static final int ACTION_ON_MUSIC_OPENERROR = ACTION_ON_MUSIC_OPENCOMPLETED + 1;
    protected static final int ACTION_ON_MUSIC_PLAING = ACTION_ON_MUSIC_OPENERROR + 1;
    protected static final int ACTION_ON_MUSIC_PAUSE = ACTION_ON_MUSIC_PLAING + 1;
    protected static final int ACTION_ON_MUSIC_STOP = ACTION_ON_MUSIC_PAUSE + 1;
    protected static final int ACTION_ON_MUSIC_COMPLETED = ACTION_ON_MUSIC_STOP + 1;
    protected static final int ACTION_ON_COUNT_DOWN = ACTION_ON_MUSIC_COMPLETED + 1;

    protected static volatile Status mStatus = Status.IDLE;

    enum Status {
        IDLE(0), Opened(1), Started(2), Paused(3), Stopped(4);

        int value;

        Status(int value) {
            this.value = value;
        }

        public boolean isAtLeast(@NonNull Status state) {
            return compareTo(state) >= 0;
        }
    }

    protected final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == ACTION_UPDATE_TIME) {
                if (mCallback != null) {
                    mCallback.onMusicPositionChanged((long) msg.obj);
                }
            } else if (msg.what == ACTION_ONMUSIC_OPENING) {
                if (mCallback != null) {
                    mCallback.onMusicOpening();
                }
            } else if (msg.what == ACTION_ON_MUSIC_OPENCOMPLETED) {
                if (mCallback != null) {
                    mCallback.onMusicOpenCompleted((long) msg.obj);
                }
            } else if (msg.what == ACTION_ON_MUSIC_OPENERROR) {
                if (mCallback != null) {
                    mCallback.onMusicOpenError((int) msg.obj);
                }
            } else if (msg.what == ACTION_ON_MUSIC_PLAING) {
                if (mCallback != null) {
                    mCallback.onMusicPlaing();
                }
            } else if (msg.what == ACTION_ON_MUSIC_PAUSE) {
                if (mCallback != null) {
                    mCallback.onMusicPause();
                }
            } else if (msg.what == ACTION_ON_MUSIC_STOP) {
                if (mCallback != null) {
                    mCallback.onMusicStop();
                }
            } else if (msg.what == ACTION_ON_MUSIC_COMPLETED) {
                if (mCallback != null) {
                    mCallback.onMusicCompleted();
                }
            } else if (msg.what == ACTION_ON_COUNT_DOWN) {
                if (mCallback != null) {
                    mCallback.onReceivedCountdown((int) msg.obj);
                }
            }
        }
    };

    public BaseMusicPlayer(Context mContext, int role, IMediaPlayer mPlayer) {
        this.mContext = mContext;
        this.mPlayer = mPlayer;
        reset();

        this.mPlayer.registerPlayerObserver(this);

        RoomManager.Instance(mContext).getRtcEngine().addHandler(this);
        switchRole(role);
    }

    private void reset() {
        mRecvedPlayPosition = 0;
        mLastRecvPlayPosTime = null;
        mMusicModel = null;
        mAudioTrackIndex = 1;
        mStatus = Status.IDLE;
    }

    public void registerPlayerObserver(Callback mCallback) {
        this.mCallback = mCallback;
    }

    public void unregisterPlayerObserver() {
        this.mCallback = null;
    }

    public abstract void switchRole(int role);

    public abstract void prepare(@NonNull MemberMusicModel music);

    public void playByListener(@NonNull MemberMusicModel mMusicModel) {
        BaseMusicPlayer.mMusicModel = mMusicModel;
        startDisplayLrc();
    }

    protected int open(@NonNull MemberMusicModel mMusicModel) {
        if (mRole != Constants.CLIENT_ROLE_BROADCASTER) {
            mLogger.e("open error: current role is not broadcaster, abort playing");
            return -1;
        }

        if (mStatus.isAtLeast(Status.Opened)) {
            mLogger.e("open error: current player is in playing state already, abort playing");
            return -2;
        }

        if (!mStopDisplayLrc) {
            mLogger.e("open error: current player is recving remote streams, abort playing");
            return -3;
        }

        File fileMusic = mMusicModel.getFileMusic();
        if (fileMusic.exists() == false) {
            mLogger.e("open error: fileMusic is not exists");
            return -4;
        }

        File fileLrc = mMusicModel.getFileLrc();
        if (fileLrc.exists() == false) {
            mLogger.e("open error: fileLrc is not exists");
            return -5;
        }

        if (mPlayer == null) {
            return -6;
        }

        stopDisplayLrc();

        mAudioTrackIndex = 1;
        BaseMusicPlayer.mMusicModel = mMusicModel;
        mLogger.i("open() called with: mMusicModel = [%s]", mMusicModel);
        mPlayer.open(fileMusic.getAbsolutePath(), 0);
        return 0;
    }

    public void stop() {
        mLogger.i("stop() called");
        if (!mStatus.isAtLeast(Status.Started)) {
            return;
        }

        mPlayer.stop();
    }

    protected void play() {
        mLogger.i("play() called");
        if (!mStatus.isAtLeast(Status.Opened)) {
            return;
        }

        if (mStatus == Status.Started)
            return;

        mStatus = Status.Started;
        mPlayer.play();
    }

    protected void pause() {
        mLogger.i("pause() called");
        if (!mStatus.isAtLeast(Status.Opened)) {
            return;
        }

        if (mStatus == Status.Paused)
            return;

        mPlayer.pause();
    }

    protected void resume() {
        mLogger.i("resume() called");
        if (!mStatus.isAtLeast(Status.Opened)) {
            return;
        }

        if (mStatus == Status.Started)
            return;

        mPlayer.resume();
    }

    public void togglePlay() {
        if (!mStatus.isAtLeast(Status.Started)) {
            return;
        }

        if (mStatus == Status.Started) {
            pause();
        } else if (mStatus == Status.Paused) {
            resume();
        }
    }

    protected int mAudioTrackIndex = 1;

    public void selectAudioTrack(int i) {
        //因为咪咕音乐没有音轨，只有左右声道，所以暂定如此
        mAudioTrackIndex = i;

        if (mAudioTrackIndex == 0) {
            mPlayer.setAudioDualMonoMode(1);
        } else {
            mPlayer.setAudioDualMonoMode(2);
        }
    }

    public boolean hasAccompaniment() {
        //因为咪咕音乐没有音轨，只有左右声道，所以暂定如此
        return true;
    }

    public void toggleOrigle() {
        if (mAudioTrackIndex == 0) {
            selectAudioTrack(1);
        } else {
            selectAudioTrack(0);
        }
    }

    public void sendCountdown(int time) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "countdown");
        msg.put("time", time);
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.Instance(mContext).getStreamId();
        RoomManager.Instance(mContext).getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
    }

    public void setMusicVolume(int v) {
        mPlayer.adjustPlayoutVolume(v);
    }

    public void setMicVolume(int v) {
        RoomManager.Instance(mContext).getRtcEngine().adjustRecordingSignalVolume(v);
    }

    public void seek(long d) {
        mPlayer.seek(d);
    }

    protected void startDisplayLrc() {
        mStopDisplayLrc = false;
        mDisplayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long curTs = 0;
                long curTime;
                long offset;
                while (!mStopDisplayLrc) {
                    if (mLastRecvPlayPosTime != null) {
                        curTime = System.currentTimeMillis();

                        offset = curTime - mLastRecvPlayPosTime;

                        if (offset <= 1000) {
                            curTs = mRecvedPlayPosition + offset;
                            mHandler.obtainMessage(ACTION_UPDATE_TIME, curTs).sendToTarget();
                        }
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException exp) {
                        break;
                    }
                }
            }
        });
        mDisplayThread.setName("Thread-Display");
        mDisplayThread.start();
    }

    protected void stopDisplayLrc() {
        mStopDisplayLrc = true;
        if (mDisplayThread != null) {
            try {
                mDisplayThread.join();
            } catch (InterruptedException exp) {
                mLogger.e("stopDisplayLrc: " + exp.getMessage());
            }
        }
    }

    private void startSyncLrc(String lrcId, long duration) {
        mSyncLrcThread = new Thread(new Runnable() {

            @Override
            public void run() {
                mLogger.i("startSyncLrc: " + lrcId);
                mStopSyncLrc = false;
                while (!mStopSyncLrc && mStatus.isAtLeast(Status.Started)) {
                    if (mPlayer == null) {
                        break;
                    }

                    if (mLastRecvPlayPosTime != null && mStatus == Status.Started) {
                        sendSyncLrc(lrcId, duration, mRecvedPlayPosition);
                    }

                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException exp) {
                        break;
                    }
                }
            }

            private void sendSyncLrc(String lrcId, long duration, long time) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("cmd", "setLrcTime");
                msg.put("lrcId", lrcId);
                msg.put("duration", duration);
                msg.put("time", time);//ms
                JSONObject jsonMsg = new JSONObject(msg);
                int streamId = RoomManager.Instance(mContext).getStreamId();
                int ret = RoomManager.Instance(mContext).getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
                if (ret < 0) {
                    mLogger.e("sendSyncLrc() sendStreamMessage called returned: ret = [%s]", ret);
                }
            }
        });
        mSyncLrcThread.setName("Thread-SyncLrc");
        mSyncLrcThread.start();
    }

    private void stopSyncLrc() {
        mStopSyncLrc = true;
        if (mSyncLrcThread != null) {
            try {
                mSyncLrcThread.join();
            } catch (InterruptedException exp) {
                mLogger.e("stopSyncLrc: " + exp.getMessage());
            }
        }
    }

    protected void startPublish() {
        startSyncLrc(mMusicModel.getMusicId(), mPlayer.getDuration());
    }

    private void stopPublish() {
        stopSyncLrc();
    }

    @Override
    public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {
        super.onStreamMessageError(uid, streamId, error, missed, cached);
        mLogger.e("onStreamMessageError() called with: uid = [%s], streamId = [%s], error = [%s], missed = [%s], cached = [%s]", uid, streamId, error, missed, cached);
    }

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
        JSONObject jsonMsg;
        try {
            String strMsg = new String(data);
            jsonMsg = new JSONObject(strMsg);

            if (jsonMsg.getString("cmd").equals("setLrcTime")) {
                long position = jsonMsg.getLong("time");
                if (position == 0) {
                    onReceivedStatusPlay(uid);
                } else if (position == -1) {
                    onReceivedStatusPause(uid);
                } else {
                    onReceivedSetLrcTime(uid, position);
                }
            } else if (jsonMsg.getString("cmd").equals("countdown")) {
                int time = jsonMsg.getInt("time");
                onReceivedCountdown(uid, time);
            } else if (jsonMsg.getString("cmd").equals("testDelay")) {
                long time = jsonMsg.getLong("time");
                onReceivedTestDelay(uid, time);
            } else if (jsonMsg.getString("cmd").equals("replyTestDelay")) {
                long testDelayTime = jsonMsg.getLong("testDelayTime");
                long time = jsonMsg.getLong("time");
                onReceivedReplyTestDelay(uid, testDelayTime, time);
            } else if (jsonMsg.getString("cmd").equals("play")) {
                long time = jsonMsg.getLong("time");
//                onReceivedStatusPlay(uid, time);
            } else if (jsonMsg.getString("cmd").equals("pause")) {
//                onReceivedStatusPause(uid);
            }
        } catch (JSONException exp) {
            mLogger.e("onStreamMessage: failed parse json, error: " + exp.toString());
        }
    }

    protected void onReceivedStatusPlay(int uid) {
        mLogger.d("onReceivedStatusPlay() called with: uid = [%s]", uid);
    }

    protected void onReceivedStatusPause(int uid) {
        mLogger.d("onReceivedStatusPause() called with: uid = [%s]", uid);
    }

    protected void onReceivedSetLrcTime(int uid, long position) {
//        mLogger.d("onReceivedSetLrcTime() called with: uid = [%s], position = [%s]", uid, position);
        mRecvedPlayPosition = position;
        mLastRecvPlayPosTime = System.currentTimeMillis();
    }

    protected void onReceivedCountdown(int uid, int time) {
        mLogger.d("onReceivedCountdown() called with: uid = [%s], time = [%s]", uid, time);
        mHandler.obtainMessage(ACTION_ON_COUNT_DOWN, time).sendToTarget();
    }

    protected void onReceivedTestDelay(int uid, long time) {
//        mLogger.d("onReceivedTestDelay() called with: uid = [%s], time = [%s]", uid, time);
    }

    protected void onReceivedReplyTestDelay(int uid, long testDelayTime, long time) {
//        mLogger.d("onReceivedReplyTestDelay() called with: uid = [%s], testDelayTime = [%s], time = [%s]", uid, testDelayTime, time);
    }

    @Override
    public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState state, io.agora.mediaplayer.Constants.MediaPlayerError error) {
        mLogger.i("onPlayerStateChanged: " + state + ", error: " + error);
        switch (state) {
            case PLAYER_STATE_OPENING:
                onMusicOpening();
                break;
            case PLAYER_STATE_OPEN_COMPLETED:
                onMusicOpenCompleted();
                break;
            case PLAYER_STATE_PLAYING:
                onMusicPlaing();
                break;
            case PLAYER_STATE_PAUSED:
                onMusicPause();
                break;
            case PLAYER_STATE_STOPPED:
                onMusicStop();
                break;
            case PLAYER_STATE_FAILED:
                onMusicOpenError(io.agora.mediaplayer.Constants.MediaPlayerError.getValue(error));
                mLogger.e("onPlayerStateChanged: failed to play, error " + error);
                break;
            default:
        }
    }

    @Override
    public void onPositionChanged(long position) {
//        mLogger.d("onPositionChanged() called with: position = [%s]", position);
        mRecvedPlayPosition = position;
        mLastRecvPlayPosTime = System.currentTimeMillis();
    }

    @Override
    public void onPlayerEvent(io.agora.mediaplayer.Constants.MediaPlayerEvent eventCode) {

    }

    @Override
    public void onMetaData(io.agora.mediaplayer.Constants.MediaPlayerMetadataType type, byte[] data) {

    }

    @Override
    public void onPlayBufferUpdated(long l) {

    }

    @Override
    public void onCompleted() {
        onMusicCompleted();
    }

    private void onMusicOpening() {
        mLogger.i("onMusicOpening() called");
        mHandler.obtainMessage(ACTION_ONMUSIC_OPENING).sendToTarget();
    }

    protected void onMusicOpenCompleted() {
        mLogger.i("onMusicOpenCompleted() called");
        mStatus = Status.Opened;

        play();
        startDisplayLrc();
        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENCOMPLETED, mPlayer.getDuration()).sendToTarget();
    }

    private void onMusicOpenError(int error) {
        mLogger.i("onMusicOpenError() called with: error = [%s]", error);
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENERROR, error).sendToTarget();
    }

    protected void onMusicPlaingByListener() {
        mLogger.i("onMusicPlaingByListener() called");
        mStatus = Status.Started;

        mHandler.obtainMessage(ACTION_ON_MUSIC_PLAING).sendToTarget();
    }

    protected void onMusicPlaing() {
        mLogger.i("onMusicPlaing() called");
        mStatus = Status.Started;

        if (mStopSyncLrc)
            startPublish();

        mHandler.obtainMessage(ACTION_ON_MUSIC_PLAING).sendToTarget();
    }

    private void onMusicPause() {
        mLogger.i("onMusicPause() called");
        mStatus = Status.Paused;

        mHandler.obtainMessage(ACTION_ON_MUSIC_PAUSE).sendToTarget();
    }

    private void onMusicStop() {
        mLogger.i("onMusicStop() called");
        mStatus = Status.Stopped;

        stopDisplayLrc();
        stopPublish();
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_STOP).sendToTarget();
    }

    private void onMusicCompleted() {
        mLogger.i("onMusicCompleted() called");
        mPlayer.stop();
        stopDisplayLrc();
        stopPublish();
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_COMPLETED).sendToTarget();
    }

    public void destory() {
        mLogger.i("destory() called");
        mPlayer.unRegisterPlayerObserver(this);
        RoomManager.Instance(mContext).getRtcEngine().removeHandler(this);
        mCallback = null;
    }

    protected void onPrepareResource() {
        if (mCallback != null) {
            mCallback.onPrepareResource();
        }
    }

    protected void onResourceReady(@NonNull MemberMusicModel music) {
        if (mCallback != null) {
            mCallback.onResourceReady(music);
        }
    }

    @MainThread
    public interface Callback {
        void onPrepareResource();

        void onResourceReady(@NonNull MemberMusicModel music);

        void onMusicOpening();

        void onMusicOpenCompleted(long duration);

        void onMusicOpenError(int error);

        void onMusicPlaing();

        void onMusicPause();

        void onMusicStop();

        void onMusicCompleted();

        void onMusicPositionChanged(long position);

        void onReceivedCountdown(int time);
    }
}
