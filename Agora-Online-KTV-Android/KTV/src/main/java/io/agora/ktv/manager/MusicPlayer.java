package io.agora.ktv.manager;

import android.content.Context;
import android.os.Bundle;
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
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.DataStreamConfig;

import static io.agora.mediaplayer.Constants.AudioDualMonoMode.AUDIO_DUAL_MONO_L;
import static io.agora.mediaplayer.Constants.AudioDualMonoMode.AUDIO_DUAL_MONO_R;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_FAILED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PAUSED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_STOPPED;

public class MusicPlayer extends IRtcEngineEventHandler {
    private Logger.Builder mLogger = XLog.tag("MusicPlayer");

    private Context mContext;
    private RtcEngine mRtcEngine;
    private IMediaPlayer player;
    private int mRole = Constants.CLIENT_ROLE_BROADCASTER;

    private boolean mStopSyncLrc = true;
    private Thread mSyncLrcThread;

    private boolean mStopDisplayLrc = true;
    private Thread mDisplayThread;

    private static volatile long mRecvedPlayPosition = 0;
    private static volatile Long mLastRecvPlayPosTime = null;

    private int mAudioTrackIndex = 1;
    private static volatile int mAudioTracksCount = 0;

    private static volatile MemberMusicModel mMusicModel;

    private Callback mCallback;

    protected static final int ACTION_UPDATE_TIME = 100;
    protected static final int ACTION_ONMUSIC_OPENING = ACTION_UPDATE_TIME + 1;
    protected static final int ACTION_ON_MUSIC_OPENCOMPLETED = ACTION_ONMUSIC_OPENING + 1;
    protected static final int ACTION_ON_MUSIC_OPENERROR = ACTION_ON_MUSIC_OPENCOMPLETED + 1;
    protected static final int ACTION_ON_MUSIC_PLAING = ACTION_ON_MUSIC_OPENERROR + 1;
    protected static final int ACTION_ON_MUSIC_PAUSE = ACTION_ON_MUSIC_PLAING + 1;
    protected static final int ACTION_ON_MUSIC_STOP = ACTION_ON_MUSIC_PAUSE + 1;
    protected static final int ACTION_ON_MUSIC_COMPLETED = ACTION_ON_MUSIC_STOP + 1;
    protected static final int ACTION_ON_RECEIVED_COUNT_DOWN = ACTION_ON_MUSIC_COMPLETED + 1;
    protected static final int ACTION_ON_RECEIVED_PLAY = ACTION_ON_RECEIVED_COUNT_DOWN + 1;
    protected static final int ACTION_ON_RECEIVED_PAUSE = ACTION_ON_RECEIVED_PLAY + 1;
    protected static final int ACTION_ON_RECEIVED_SYNC_TIME = ACTION_ON_RECEIVED_PAUSE + 1;
    protected static final int ACTION_ON_RECEIVED_TEST_DELAY = ACTION_ON_RECEIVED_SYNC_TIME + 1;
    protected static final int ACTION_ON_RECEIVED_REPLAY_TEST_DELAY = ACTION_ON_RECEIVED_TEST_DELAY + 1;
    protected static final int ACTION_ON_RECEIVED_CHANGED_ORIGLE = ACTION_ON_RECEIVED_REPLAY_TEST_DELAY + 1;

    private static volatile Status mStatus = Status.IDLE;

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

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
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
                    mCallback.onMusicOpenCompleted(Long.valueOf((long) msg.obj).intValue());
                }
            } else if (msg.what == ACTION_ON_MUSIC_OPENERROR) {
                if (mCallback != null) {
                    mCallback.onMusicOpenError(Long.valueOf((long) msg.obj).intValue());
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
            } else if (msg.what == ACTION_ON_RECEIVED_SYNC_TIME) {
                Bundle data = msg.getData();
                int uid = data.getInt("uid");
                long time = data.getLong("time");
                onReceivedSetLrcTime(uid, time);
            }
        }
    };

    public MusicPlayer(Context mContext, RtcEngine mRtcEngine) {
        this.mContext = mContext;
        this.mRtcEngine = mRtcEngine;
        this.player = mRtcEngine.createMediaPlayer();
        this.player.registerPlayerObserver(iMediaPlayerObserver);
        reset();

        mRtcEngine.addHandler(this);
    }

    private void reset() {
        mAudioTracksCount = 0;
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

    public void switchRole(int role) {
        mLogger.d("switchRole() called with: role = [%s]", role);
        mRole = role;
    }

    public void playByListener(@NonNull MemberMusicModel mMusicModel) {
        onMusicPlaingByListener();
        MusicPlayer.mMusicModel = mMusicModel;
        startDisplayLrc();
    }

    public int open(@NonNull MemberMusicModel mMusicModel) {
        if (mRole != Constants.CLIENT_ROLE_BROADCASTER) {
            mLogger.e("play: current role is not broadcaster, abort playing");
            return -1;
        }

        if (mStatus.isAtLeast(Status.Opened)) {
            mLogger.e("play: current player is in playing state already, abort playing");
            return -2;
        }

        if (!mStopDisplayLrc) {
            mLogger.e("play: current player is recving remote streams, abort playing");
            return -3;
        }

        File fileMusic = mMusicModel.getFileMusic();
        if (fileMusic.exists() == false) {
            mLogger.e("play: fileMusic is not exists");
            return -4;
        }

        File fileLrc = mMusicModel.getFileLrc();
        if (fileLrc.exists() == false) {
            mLogger.e("play: fileLrc is not exists");
            return -5;
        }

        stopDisplayLrc();

        mAudioTracksCount = 0;
        mAudioTrackIndex = 1;
        MusicPlayer.mMusicModel = mMusicModel;
        mLogger.i("play() called with: mMusicModel = [%s]", mMusicModel);
        onMusicOpening();
        int ret = player.open(fileMusic.getAbsolutePath(), 0);
        mLogger.i("play() called ret= %s", ret);
        return 0;
    }

    protected void play() {

    }

    public void stop() {
        mLogger.i("stop() called");
        if (mStatus == Status.IDLE) {
            return;
        }

        player.stop();
    }

    private void pause() {
        mLogger.i("pause() called");
        if (!mStatus.isAtLeast(Status.Started))
            return;

        player.pause();
    }

    private void resume() {
        mLogger.i("resume() called");
        if (!mStatus.isAtLeast(Status.Started))
            return;

        player.resume();
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

    public void selectAudioTrack(int i) {
        if (i < 0 || mAudioTracksCount == 0 || i >= mAudioTracksCount)
            return;

        mAudioTrackIndex = i;

        if (mMusicModel.getType() == MemberMusicModel.Type.Default) {
            player.selectAudioTrack(mAudioTrackIndex);
        } else {
            if (mAudioTrackIndex == 0) {
                player.setAudioDualMonoMode(AUDIO_DUAL_MONO_L);
            } else {
                player.setAudioDualMonoMode(AUDIO_DUAL_MONO_R);
            }
        }
    }

    public void seek(long time) {
        player.seek(time);
    }

    public boolean hasAccompaniment() {
        return mAudioTracksCount >= 2;
    }

    public void toggleOrigle() {
        if (mAudioTrackIndex == 0) {
            selectAudioTrack(1);
        } else {
            selectAudioTrack(0);
        }
    }

    public void setMusicVolume(int v) {
        player.adjustPublishSignalVolume(v);
        player.adjustPlayoutVolume(v);
    }

    public void setMicVolume(int v) {
        mRtcEngine.adjustRecordingSignalVolume(v);
    }

    private void startDisplayLrc() {
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

    private void stopDisplayLrc() {
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
            int mStreamId = -1;

            @Override
            public void run() {
                mLogger.i("startSyncLrc: " + lrcId);
                DataStreamConfig cfg = new DataStreamConfig();
                cfg.syncWithAudio = true;
                cfg.ordered = true;
                mStreamId = mRtcEngine.createDataStream(cfg);

                mStopSyncLrc = false;
                while (!mStopSyncLrc && mStatus.isAtLeast(Status.Started)) {
                    if (mStatus == Status.Started) {
                        mRecvedPlayPosition = player.getPlayPosition();
                        mLastRecvPlayPosTime = System.currentTimeMillis();

                        sendSyncLrc(lrcId, duration, mRecvedPlayPosition);
                    }

                    try {
                        Thread.sleep(999L);
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
                mRtcEngine.sendStreamMessage(mStreamId, jsonMsg.toString().getBytes());
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

    private void startPublish() {
        startSyncLrc(mMusicModel.getMusicId(), player.getDuration());
    }

    private void stopPublish() {
        stopSyncLrc();
    }

    private void initAudioTracks() {
        mAudioTrackIndex = 1;
        mAudioTracksCount = 2;
    }

    public boolean isPlaying() {
        return mStatus.isAtLeast(Status.Started);
    }

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
        JSONObject jsonMsg;
        try {
            String strMsg = new String(data);
            jsonMsg = new JSONObject(strMsg);

            if (mStatus.isAtLeast(Status.Started))
                return;

            if (jsonMsg.getString("cmd").equals("setLrcTime")) {
                long position = jsonMsg.getLong("time");
                if (position == 0) {
                    mHandler.obtainMessage(ACTION_ON_RECEIVED_PLAY, uid).sendToTarget();
                } else if (position == -1) {
                    mHandler.obtainMessage(ACTION_ON_RECEIVED_PAUSE, uid).sendToTarget();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putInt("uid", uid);
                    bundle.putLong("time", position);
                    Message message = Message.obtain(mHandler, ACTION_ON_RECEIVED_SYNC_TIME);
                    message.setData(bundle);
                    message.sendToTarget();
                }
            }
        } catch (JSONException exp) {
            mLogger.e("onStreamMessage: failed parse json, error: " + exp.toString());
        }
    }

    protected void onReceivedSetLrcTime(int uid, long position) {
        mRecvedPlayPosition = position;
        mLastRecvPlayPosTime = System.currentTimeMillis();
    }

    private void onMusicOpening() {
        mLogger.i("onMusicOpening() called");
        mHandler.obtainMessage(ACTION_ONMUSIC_OPENING).sendToTarget();
    }

    private void onMusicOpenCompleted() {
        mLogger.i("onMusicOpenCompleted() called");
        mStatus = Status.Opened;

        initAudioTracks();

        startDisplayLrc();
        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENCOMPLETED, player.getDuration()).sendToTarget();
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

    private void onMusicPlaing() {
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
        stopDisplayLrc();
        stopPublish();
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_COMPLETED).sendToTarget();
    }

    public void destory() {
        mLogger.i("destory() called");
        mRtcEngine.removeHandler(this);
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
        /**
         * 从云端下载资源
         */
        void onPrepareResource();

        /**
         * 资源下载结束
         *
         * @param music
         */
        void onResourceReady(@NonNull MemberMusicModel music);

        /**
         * 歌曲文件打开
         */
        void onMusicOpening();

        /**
         * 歌曲打开成功
         *
         * @param duration 总共时间，毫秒
         */
        void onMusicOpenCompleted(int duration);

        /**
         * 歌曲打开失败
         *
         * @param error 错误码
         */
        void onMusicOpenError(int error);

        /**
         * 正在播放
         */
        void onMusicPlaing();

        /**
         * 暂停
         */
        void onMusicPause();

        /**
         * 结束
         */
        void onMusicStop();

        /**
         * 播放完成
         */
        void onMusicCompleted();

        /**
         * 进度更新
         *
         * @param position
         */
        void onMusicPositionChanged(long position);
    }

    private IMediaPlayerObserver iMediaPlayerObserver = new IMediaPlayerObserver() {
        @Override
        public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState state, io.agora.mediaplayer.Constants.MediaPlayerError error) {
            mLogger.d("onPlayerStateChanged() called with: state = [%s], errorCode = [%s]", state, error);
            if (state == PLAYER_STATE_PLAYING) {
                if (mStatus == Status.IDLE) {
                    onMusicOpenCompleted();
                }
                onMusicPlaing();
            } else if(state == PLAYER_STATE_OPEN_COMPLETED){
                player.play();
            } else if (state == PLAYER_STATE_PAUSED) {
                onMusicPause();
            } else if (state == PLAYER_STATE_STOPPED) {
                onMusicStop();
                onMusicCompleted();
            } else if (state == PLAYER_STATE_FAILED) {
                onMusicOpenError(error.ordinal());
            }
        }

        @Override
        public void onPositionChanged(long position) {

        }

        @Override
        public void onPlayerEvent(io.agora.mediaplayer.Constants.MediaPlayerEvent eventCode, long elapsedTime, String message) {

        }

        @Override
        public void onMetaData(io.agora.mediaplayer.Constants.MediaPlayerMetadataType type, byte[] data) {

        }

        @Override
        public void onPlayBufferUpdated(long playCachedBuffer) {

        }

        @Override
        public void onPreloadEvent(String src, io.agora.mediaplayer.Constants.MediaPlayerPreloadEvent event) {

        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onAgoraCDNTokenWillExpire() {

        }
    };
}
