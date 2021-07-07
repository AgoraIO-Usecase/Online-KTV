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
import io.agora.lrcview.LrcView;
import io.agora.mediaplayer.AudioFrameObserver;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.mediaplayer.data.AudioFrame;
import io.agora.mediaplayer.data.MediaStreamInfo;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class MusicPlayer extends IRtcEngineEventHandler implements IMediaPlayerObserver {
    private Logger.Builder mLogger = XLog.tag("MusicPlayer");

    private Context mContext;
    private RtcEngine mRtcEngine;
    private LrcView mLrcView;
    private int mRole = Constants.CLIENT_ROLE_BROADCASTER;

    private boolean mStopSyncLrc = true;
    private Thread mSyncLrcThread;

    private boolean mStopDisplayLrc = true;
    private Thread mDisplayThread;

    private IMediaPlayer mPlayer;
    private volatile boolean mIsPlaying = false;
    private volatile boolean mIsPaused = false;

    private static volatile long mRecvedPlayPosition = 0;
    private static volatile Long mLastRecvPlayPosTime = null;

    private static volatile int mAudioTracksCount = 0;
    private int[] mAudioTrackIndices = null;

    private static volatile MemberMusicModel mMusicModelOpen;
    private static volatile MemberMusicModel mMusicModel;

    private Callback mCallback;

    private static final int ACTION_UPDATE_TIME = 102;

    private static final int ACTION_ONMUSIC_OPENING = 200;
    private static final int ACTION_ON_MUSIC_OPENCOMPLETED = 201;
    private static final int ACTION_ON_MUSIC_OPENERROR = 202;
    private static final int ACTION_ON_MUSIC_PLAING = 203;
    private static final int ACTION_ON_MUSIC_PAUSE = 204;
    private static final int ACTION_ON_MUSIC_STOP = 205;
    private static final int ACTION_ON_MUSIC_COMPLETED = 206;
    private static final int ACTION_ON_MUSIC_PREPARING = 207;
    private static final int ACTION_ON_MUSIC_PREPARED = 208;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == ACTION_UPDATE_TIME) {
                mLrcView.updateTime((long) msg.obj);
            } else if (msg.what == ACTION_ONMUSIC_OPENING) {
                if (mCallback != null) {
                    mCallback.onMusicOpening();
                }
            } else if (msg.what == ACTION_ON_MUSIC_OPENCOMPLETED) {
                if (mCallback != null) {
                    mCallback.onMusicOpenCompleted();
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
            } else if (msg.what == ACTION_ON_MUSIC_PREPARING) {
                if (mCallback != null) {
                    mCallback.onMusicPlaing();
                }
            } else if (msg.what == ACTION_ON_MUSIC_PREPARED) {
                if (mCallback != null) {
                    mCallback.onMusicPrepared();
                }
            }
        }
    };

    private final AudioFrameObserver mAudioFrameObserver = new AudioFrameObserver() {
        @Override
        public AudioFrame onFrame(AudioFrame audioFrame) {
            if (mMusicModel == null || mMusicModel.getType() == MemberMusicModel.Type.Default) {
                return audioFrame;
            }

            int channelNums = audioFrame.channelNums;
            mAudioTracksCount = channelNums;

            if (mAudioTrackIndex == 0 && channelNums == 2) {
                int bytesPerSample = audioFrame.bytesPerSample;
                int samplesPerChannel = audioFrame.samplesPerChannel;

                byte[] tempBuf = new byte[audioFrame.bytes.length];
                int cpBytes = bytesPerSample / channelNums;
                for (int i = 0; i < samplesPerChannel; i++) {
                    System.arraycopy(audioFrame.bytes, i * bytesPerSample, tempBuf, 0, cpBytes);
                    System.arraycopy(audioFrame.bytes, i * bytesPerSample + cpBytes, audioFrame.bytes, i * bytesPerSample + cpBytes, cpBytes);
                    System.arraycopy(tempBuf, 0, audioFrame.bytes, i * bytesPerSample + cpBytes, cpBytes);
                }
            }
            return audioFrame;
        }
    };

    public MusicPlayer(Context mContext, RtcEngine mRtcEngine, LrcView lrcView) {
        this.mContext = mContext;
        this.mRtcEngine = mRtcEngine;
        this.mLrcView = lrcView;

        reset();

        // init mpk
        mPlayer = mRtcEngine.createMediaPlayer();
        mPlayer.registerPlayerObserver(this);
        mPlayer.registerAudioFrameObserver(mAudioFrameObserver, Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE);

        mRtcEngine.addHandler(this);
    }

    private void reset() {
        mIsPlaying = false;
        mIsPaused = false;
        mAudioTracksCount = 0;
        mAudioTrackIndices = null;
        mRecvedPlayPosition = 0;
        mLastRecvPlayPosTime = null;
        mMusicModelOpen = null;
        mMusicModel = null;
        mAudioTrackIndex = 1;
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

        ChannelMediaOptions options = new ChannelMediaOptions();

        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
        options.clientRoleType = role;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = false;
        options.publishCameraTrack = false;
        options.publishMediaPlayerVideoTrack = false;
        if (role == Constants.CLIENT_ROLE_BROADCASTER) {
            options.publishAudioTrack = true;
            options.publishCustomAudioTrack = false;
            options.enableAudioRecordingOrPlayout = true;
            options.publishMediaPlayerAudioTrack = true;
        } else {
            options.publishAudioTrack = false;
            options.publishCustomAudioTrack = false;
            options.enableAudioRecordingOrPlayout = false;
            options.publishMediaPlayerAudioTrack = false;
        }
        mRtcEngine.updateChannelMediaOptions(options);
    }

    public int play(MemberMusicModel mMusicModel) {
        if (mRole != Constants.CLIENT_ROLE_BROADCASTER) {
            mLogger.e("play: current role is not broadcaster, abort playing");
            return -1;
        }

        if (mIsPlaying) {
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

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
        options.clientRoleType = mRole;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = false;
        options.publishCameraTrack = false;
        options.publishMediaPlayerVideoTrack = false;
        options.publishAudioTrack = true;
        options.publishCustomAudioTrack = false;
        options.enableAudioRecordingOrPlayout = true;
        options.publishMediaPlayerAudioTrack = true;
        mRtcEngine.updateChannelMediaOptions(options);

        stopDisplayLrc();
        // mpk open file
        mAudioTracksCount = 0;
        mAudioTrackIndex = 1;
        mAudioTrackIndices = null;
        MusicPlayer.mMusicModelOpen = mMusicModel;
        mLogger.i("play() called with: mMusicModel = [%s]", mMusicModel);
        mPlayer.open(fileMusic.getAbsolutePath(), 0);
        return 0;
    }

    private volatile CompletableEmitter emitterStop;

    public Completable stop() {
        mLogger.i("stop() called");
        if (isPlaying() == false) {
            return Completable.complete();
        }

        return Completable.create(emitter -> {
            this.emitterStop = emitter;

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
            options.clientRoleType = mRole;
            options.autoSubscribeAudio = true;
            options.autoSubscribeVideo = false;
            options.publishCameraTrack = false;
            options.publishMediaPlayerVideoTrack = false;
            options.publishAudioTrack = false;
            options.publishCustomAudioTrack = false;
            options.enableAudioRecordingOrPlayout = false;
            options.publishMediaPlayerAudioTrack = false;
            mRtcEngine.updateChannelMediaOptions(options);
            // mpk stop
            mPlayer.stop();
        });
    }

    public void pause() {
        mLogger.i("pause() called");
        if (!mIsPlaying)
            return;
        mPlayer.pause();
    }

    public void resume() {
        mLogger.i("resume() called");
        if (!mIsPlaying)
            return;
        mPlayer.resume();
    }

    private int mAudioTrackIndex = 1;

    public void selectAudioTrack(int i) {
        if (i < 0 || mAudioTracksCount == 0 || i >= mAudioTracksCount)
            return;

        mAudioTrackIndex = i;

        if (mMusicModel.getType() == MemberMusicModel.Type.Default) {
            mPlayer.selectAudioTrack(mAudioTrackIndices[i]);
        }
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
        mPlayer.adjustPlayoutVolume(v);
    }

    public void setMicVolume(int v) {
        mRtcEngine.adjustRecordingSignalVolume(v);
    }

    private void startDisplayLrc(long totalDuration) {
        File lrcs = mMusicModel.getFileLrc();
        mLrcView.post(new Runnable() {
            @Override
            public void run() {
                mLrcView.setTotalDuration(totalDuration);
                mLrcView.loadLrc(lrcs);
            }
        });

        mStopDisplayLrc = false;
        mDisplayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long curTs;
                long curTime;
                long offset;
                while (!mStopDisplayLrc) {
                    if (!isPaused()) {
                        if (mLastRecvPlayPosTime != null) {
                            curTime = System.currentTimeMillis();
                            offset = curTime - mLastRecvPlayPosTime;
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
        mLrcView.post(new Runnable() {
            @Override
            public void run() {
                mLrcView.reset();
            }
        });
        mMusicModel = null;
    }

    private void startSyncLrc(String lrcId, long duration) {
        mSyncLrcThread = new Thread(new Runnable() {
            int mStreamId = -1;
            long lrcDuration = 0;

            @Override
            public void run() {
                lrcDuration = duration;

                mLogger.i("startSyncLrc: " + lrcId);
                DataStreamConfig cfg = new DataStreamConfig();
                cfg.syncWithAudio = false;
                cfg.ordered = false;
                mStreamId = mRtcEngine.createDataStream(cfg);

                mStopSyncLrc = false;
                while (!mStopSyncLrc && mIsPlaying) {
                    if (!mIsPaused)
                        sendSyncLrc(lrcId, lrcDuration, mPlayer.getPlayPosition());

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException exp) {
                        break;
                    }
                }

                sendMusicStop(lrcId);
                mLogger.i("stoppedSyncLrc: " + lrcId);
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

            private void sendMusicStop(String lrcId) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("cmd", "musicStopped");
                msg.put("lrcId", lrcId);
                JSONObject jsonMsg = new JSONObject(msg);
                mRtcEngine.sendStreamMessage(mStreamId, jsonMsg.toString().getBytes());
            }
        });
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
        startSyncLrc(mMusicModel.getMusicId(), mPlayer.getDuration());
    }

    private void stopPublish() {
        stopSyncLrc();
    }

    private void initAudioTracks() {
        if (mMusicModel.getType() != MemberMusicModel.Type.Default) {
            return;
        }

        int nt = mPlayer.getStreamCount();
        int nat = 0;
        mAudioTrackIndices = new int[nt];
        for (int i = 0; i < nt; ++i) {
            MediaStreamInfo stmInfo = mPlayer.getStreamInfo(i);
            if (stmInfo != null && stmInfo.getMediaStreamType() ==
                    io.agora.mediaplayer.Constants.MediaStreamType.getValue(
                            io.agora.mediaplayer.Constants.MediaStreamType.STREAM_TYPE_AUDIO)) {
                mAudioTrackIndices[nat] = stmInfo.getStreamIndex();
                ++nat;
            }
        }
        mAudioTracksCount = nat;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public boolean isPaused() {
        return mIsPaused;
    }

    private static volatile boolean isPreparing = false;

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
        JSONObject jsonMsg;
        try {
            String strMsg = new String(data);
            jsonMsg = new JSONObject(strMsg);
            mLogger.i("onStreamMessage: recv msg: " + strMsg);
            if (mIsPlaying)
                return;

            if (jsonMsg.getString("cmd").equals("setLrcTime")) {
                if (mMusicModel == null || !jsonMsg.getString("lrcId").equals(mMusicModel.getMusicId())) {
                    if (isPreparing) {
                        return;
                    }

                    stopDisplayLrc();

                    // 加载歌词文本
                    String musicId = jsonMsg.getString("lrcId");
                    long duration = jsonMsg.getLong("duration");

                    onMusicPreparing();
                    MemberMusicModel musicModel = new MemberMusicModel(musicId);
                    MusicResourceManager.Instance(mContext)
                            .prepareMusic(musicModel)
                            .subscribe(new SingleObserver<MemberMusicModel>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onSuccess(@NonNull MemberMusicModel musicModel) {
                                    onMusicPrepared();

                                    MusicPlayer.mMusicModel = musicModel;
                                    startDisplayLrc(duration);
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    isPreparing = false;
                                }
                            });
                }
                mRecvedPlayPosition = jsonMsg.getLong("time");
                mLastRecvPlayPosTime = System.currentTimeMillis();
            } else if (jsonMsg.getString("cmd").equals("musicStopped")) {
                stopDisplayLrc();
            }
        } catch (JSONException exp) {
            mLogger.e("onStreamMessage: failed parse json, error: " + exp.toString());
        }
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
        mLogger.d("onPositionChanged: position: " + position + ", duration: " + mPlayer.getDuration());
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

    private void onMusicOpenCompleted() {
        mLogger.i("onMusicOpenCompleted() called");
        MusicPlayer.mMusicModel = mMusicModelOpen;
        mMusicModelOpen = null;

        initAudioTracks();

        mIsPlaying = true;
        mPlayer.play();
        startDisplayLrc(mPlayer.getDuration() * 1000);
        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENCOMPLETED).sendToTarget();
    }

    private void onMusicOpenError(int error) {
        mLogger.i("onMusicOpenError() called with: error = [%s]", error);
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENERROR, error).sendToTarget();
    }

    private void onMusicPlaing() {
        mLogger.i("onMusicPlaing() called");
        mIsPaused = false;
        if (mStopSyncLrc)
            startPublish();

        mHandler.obtainMessage(ACTION_ON_MUSIC_PLAING).sendToTarget();
    }

    private void onMusicPause() {
        mLogger.i("onMusicPause() called");
        mIsPaused = true;

        mHandler.obtainMessage(ACTION_ON_MUSIC_PAUSE).sendToTarget();
    }

    private void onMusicStop() {
        mLogger.i("onMusicStop() called");
        stopDisplayLrc();
        stopPublish();
        reset();

        if (emitterStop != null) {
            emitterStop.onComplete();
        }

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

    private void onMusicPreparing() {
        mLogger.i("onMusicPreparing() called");
        isPreparing = true;
        mHandler.obtainMessage(ACTION_ON_MUSIC_COMPLETED).sendToTarget();
    }

    private void onMusicPrepared() {
        mLogger.i("onMusicPrepared() called");
        isPreparing = false;
        mHandler.obtainMessage(ACTION_ON_MUSIC_COMPLETED).sendToTarget();
    }

    public void destory() {
        mLogger.i("destory() called");
        mRtcEngine.removeHandler(this);
        mCallback = null;

        mPlayer.registerAudioFrameObserver(mAudioFrameObserver, Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE);
        mPlayer.destroy();
        mPlayer = null;
    }

    @MainThread
    public interface Callback {
        void onMusicOpening();

        void onMusicOpenCompleted();

        void onMusicOpenError(int error);

        void onMusicPlaing();

        void onMusicPause();

        void onMusicStop();

        void onMusicCompleted();

        void onMusicPreparing();

        void onMusicPrepared();
    }
}
