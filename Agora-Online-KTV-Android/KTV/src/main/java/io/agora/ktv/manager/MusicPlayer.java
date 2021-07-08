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
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.DataStreamConfig;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class MusicPlayer extends IRtcEngineEventHandler {
    private Logger.Builder mLogger = XLog.tag("MusicPlayer");

    private Context mContext;
    private RtcEngine mRtcEngine;
    private LrcView mLrcView;
    private int mRole = Constants.CLIENT_ROLE_BROADCASTER;

    private boolean mStopSyncLrc = true;
    private Thread mSyncLrcThread;

    private boolean mStopDisplayLrc = true;
    private Thread mDisplayThread;

    private static volatile long mRecvedPlayPosition = 0;
    private static volatile Long mLastRecvPlayPosTime = null;

    private int mAudioTrackIndex = 1;
    private static volatile int mAudioTracksCount = 0;

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
    private static final int ACTION_ON_MUSIC_PREPARE_FAIL = 209;

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
            } else if (msg.what == ACTION_ON_MUSIC_PREPARE_FAIL) {
                if (mCallback != null) {
                    mCallback.onMusicPrepareError();
                }
            }
        }
    };

    public MusicPlayer(Context mContext, RtcEngine mRtcEngine, LrcView lrcView) {
        this.mContext = mContext;
        this.mRtcEngine = mRtcEngine;
        this.mLrcView = lrcView;

        reset();

        mRtcEngine.addHandler(this);
    }

    private void reset() {
        mAudioTracksCount = 0;
        mRecvedPlayPosition = 0;
        mLastRecvPlayPosTime = null;
        mMusicModelOpen = null;
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

    public void playWithDisplay(MemberMusicModel mMusicModel) {
        MusicPlayer.mMusicModel = mMusicModel;
        startDisplayLrc();
    }

    public int play(MemberMusicModel mMusicModel) {
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
        MusicPlayer.mMusicModelOpen = mMusicModel;
        mLogger.i("play() called with: mMusicModel = [%s]", mMusicModel);
        onMusicOpening();
        int ret = mRtcEngine.startAudioMixing(fileMusic.getAbsolutePath(), false, false, 1);
        mLogger.i("play() called ret= %s", ret);
        return 0;
    }

    private volatile CompletableEmitter emitterStop;

    public Completable stop() {
        mLogger.i("stop() called");
        if (mStatus == Status.IDLE) {
            return Completable.complete();
        }

        return Completable.create(emitter -> {
            this.emitterStop = emitter;
            mRtcEngine.stopAudioMixing();
        });
    }

    private void pause() {
        mLogger.i("pause() called");
        if (!mStatus.isAtLeast(Status.Started))
            return;

        mRtcEngine.pauseAudioMixing();
    }

    private void resume() {
        mLogger.i("resume() called");
        if (!mStatus.isAtLeast(Status.Started))
            return;

        mRtcEngine.resumeAudioMixing();
    }

    public void toggleStart() {
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
            mRtcEngine.selectAudioTrack(mAudioTrackIndex);
        } else {
            if (mAudioTrackIndex == 0) {
                mRtcEngine.setAudioMixingDualMonoMode(Constants.AudioMixingDualMonoMode.getValue(Constants.AudioMixingDualMonoMode.AUDIO_MIXING_DUAL_MONO_L));
            } else {
                mRtcEngine.setAudioMixingDualMonoMode(Constants.AudioMixingDualMonoMode.getValue(Constants.AudioMixingDualMonoMode.AUDIO_MIXING_DUAL_MONO_R));
            }
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
        mRtcEngine.adjustAudioMixingPlayoutVolume(v);
    }

    public void setMicVolume(int v) {
        mRtcEngine.adjustRecordingSignalVolume(v);
    }

    private void startDisplayLrc() {
        File lrcs = mMusicModel.getFileLrc();
        mLrcView.post(new Runnable() {
            @Override
            public void run() {
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
                    if (mLastRecvPlayPosTime != null) {
                        curTime = System.currentTimeMillis();
                        offset = curTime - mLastRecvPlayPosTime;
                        curTs = mRecvedPlayPosition + offset;

                        mHandler.obtainMessage(ACTION_UPDATE_TIME, curTs).sendToTarget();
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

            @Override
            public void run() {
                mLogger.i("startSyncLrc: " + lrcId);
                DataStreamConfig cfg = new DataStreamConfig();
                cfg.syncWithAudio = false;
                cfg.ordered = false;
                mStreamId = mRtcEngine.createDataStream(cfg);

                mStopSyncLrc = false;
                while (!mStopSyncLrc && mStatus.isAtLeast(Status.Started)) {
                    if (mStatus == Status.Started) {
                        mRecvedPlayPosition = mRtcEngine.getAudioMixingCurrentPosition();
                        mLastRecvPlayPosTime = System.currentTimeMillis();

                        sendSyncLrc(lrcId, duration, mRecvedPlayPosition);
                    }

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
        startSyncLrc(mMusicModel.getMusicId(), mRtcEngine.getAudioMixingDuration());
    }

    private void stopPublish() {
        stopSyncLrc();
    }

    private void initAudioTracks() {
        mAudioTrackIndex = 1;
//        mAudioTracksCount = mRtcEngine.getAudioTrackCount();
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
            mLogger.i("onStreamMessage: recv msg: " + strMsg);
            if (mStatus.isAtLeast(Status.Started))
                return;

            if (jsonMsg.getString("cmd").equals("setLrcTime")) {
                if (mMusicModel == null || !jsonMsg.getString("lrcId").equals(mMusicModel.getMusicId())) {
                    if (MusicResourceManager.isPreparing) {
                        return;
                    }

                    stopDisplayLrc();

                    // 加载歌词文本
                    String musicId = jsonMsg.getString("lrcId");
                    long duration = jsonMsg.getLong("duration");

                    onMusicPreparing();
                    MemberMusicModel musicModel = new MemberMusicModel(musicId);
                    MusicResourceManager.Instance(mContext)
                            .prepareMusic(musicModel, true)
                            .subscribe(new SingleObserver<MemberMusicModel>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onSuccess(@NonNull MemberMusicModel musicModel) {
                                    onMusicPrepared();
                                    playWithDisplay(musicModel);
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    onMusicPrepareError();
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
    public void onAudioMixingStateChanged(int state, int errorCode) {
        super.onAudioMixingStateChanged(state, errorCode);
        mLogger.d("onAudioMixingStateChanged() called with: state = [%s], errorCode = [%s]", state, errorCode);
        if (state == Constants.MEDIA_ENGINE_AUDIO_EVENT_MIXING_PLAY) {
            if (mStatus == Status.IDLE) {
                onMusicOpenCompleted();
            }
            onMusicPlaing();
        } else if (state == Constants.MEDIA_ENGINE_AUDIO_EVENT_MIXING_PAUSED) {
            onMusicPause();
        } else if (state == Constants.MEDIA_ENGINE_AUDIO_EVENT_MIXING_STOPPED) {
            onMusicStop();
            onMusicCompleted();
        } else if (state == Constants.MEDIA_ENGINE_AUDIO_EVENT_MIXING_ERROR) {
            onMusicOpenError(errorCode);
        }
    }

    private void onMusicOpening() {
        mLogger.i("onMusicOpening() called");
        mHandler.obtainMessage(ACTION_ONMUSIC_OPENING).sendToTarget();
    }

    private void onMusicOpenCompleted() {
        mLogger.i("onMusicOpenCompleted() called");
        mStatus = Status.Opened;

        MusicPlayer.mMusicModel = mMusicModelOpen;
        mMusicModelOpen = null;

        initAudioTracks();

        mLrcView.setTotalDuration(mRtcEngine.getAudioMixingDuration());
        startDisplayLrc();
        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENCOMPLETED).sendToTarget();
    }

    private void onMusicOpenError(int error) {
        mLogger.i("onMusicOpenError() called with: error = [%s]", error);
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENERROR, error).sendToTarget();
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

        if (emitterStop != null) {
            emitterStop.onComplete();
        }

        mHandler.obtainMessage(ACTION_ON_MUSIC_STOP).sendToTarget();
    }

    private void onMusicCompleted() {
        mLogger.i("onMusicCompleted() called");
        stopDisplayLrc();
        stopPublish();
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_COMPLETED).sendToTarget();
    }

    private void onMusicPreparing() {
        mLogger.i("onMusicPreparing() called");
        mHandler.obtainMessage(ACTION_ON_MUSIC_PREPARING).sendToTarget();
    }

    private void onMusicPrepared() {
        mLogger.i("onMusicPrepared() called");
        mHandler.obtainMessage(ACTION_ON_MUSIC_PREPARED).sendToTarget();
    }

    private void onMusicPrepareError() {
        mLogger.i("onMusicPrepareError() called");
        mHandler.obtainMessage(ACTION_ON_MUSIC_PREPARE_FAIL).sendToTarget();
    }

    public void destory() {
        mLogger.i("destory() called");
        mRtcEngine.removeHandler(this);
        mCallback = null;
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

        void onMusicPrepareError();
    }
}
