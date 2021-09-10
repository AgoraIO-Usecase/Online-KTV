package io.agora.ktv.manager;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.User;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.DataStreamConfig;

public class MusicPlayer extends IRtcEngineEventHandler {
    private Logger.Builder mLogger = XLog.tag("MusicPlayer");

    private Context mContext;
    private RtcEngine mRtcEngine;
    private int mRole = Constants.CLIENT_ROLE_BROADCASTER;

    private boolean mStopSyncLrc = true;
    private Thread mSyncLrcThread;

    private boolean mStopDisplayLrc = false;
    private Thread mDisplayThread;

    private static volatile long mRecvedPlayPosition = 0;
    private static volatile Long mLastRecvPlayPosTime = null;

    private int mAudioTrackIndex = 1;
    private static volatile int mAudioTracksCount = 0;

    private static volatile MemberMusicModel mMusicModel;

    private Callback mCallback;

    private int mStreamId = -1;

    protected static final int ACTION_UPDATE_TIME = 100;
    protected static final int ACTION_ONMUSIC_OPENING = ACTION_UPDATE_TIME + 1;
    protected static final int ACTION_ON_MUSIC_OPENCOMPLETED = ACTION_ONMUSIC_OPENING + 1;
    protected static final int ACTION_ON_MUSIC_OPENERROR = ACTION_ON_MUSIC_OPENCOMPLETED + 1;
    protected static final int ACTION_ON_MUSIC_PLAYING = ACTION_ON_MUSIC_OPENERROR + 1;
    protected static final int ACTION_ON_MUSIC_PAUSE = ACTION_ON_MUSIC_PLAYING + 1;
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
                    mCallback.onMusicOpenCompleted((int) msg.obj);
                }
            } else if (msg.what == ACTION_ON_MUSIC_OPENERROR) {
                if (mCallback != null) {
                    mCallback.onMusicOpenError((int) msg.obj);
                }
            } else if (msg.what == ACTION_ON_MUSIC_PLAYING) {
                if (mCallback != null) {
                    mCallback.onMusicPlaying();
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
        onMusicPlayingByListener();
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

//        if (!mStopDisplayLrc) {
//            mLogger.e("play: current player is recving remote streams, abort playing");
//            return -3;
//        }

        File fileMusic = mMusicModel.getFileMusic();
        if (!fileMusic.exists()) {
            mLogger.e("play: fileMusic is not exists");
            return -4;
        }

        File fileLrc = mMusicModel.getFileLrc();
        if (!fileLrc.exists()) {
            mLogger.e("play: fileLrc is not exists");
            return -5;
        }

//        stopDisplayLrc();

        mAudioTracksCount = 0;
        mAudioTrackIndex = 1;
        MusicPlayer.mMusicModel = mMusicModel;
        mLogger.i("play() called with: mMusicModel = [%s]", mMusicModel);
        onMusicOpening();
        int ret = mRtcEngine.startAudioMixing(fileMusic.getAbsolutePath(), false, false, 1);
        mLogger.i("play() called ret= %s", ret);
        return 0;
    }

    public void stop() {
        mLogger.i("stop() called");
        if (mStatus == Status.IDLE) {
            return;
        }

        if(mMusicModel!=null)
            sendSyncLrc(mMusicModel.getMusicId(), mRtcEngine.getAudioMixingDuration(),mRtcEngine.getAudioMixingCurrentPosition(),false);
        mRtcEngine.stopAudioMixing();
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
            mRtcEngine.selectAudioTrack(mAudioTrackIndex);
        } else {
            if (mAudioTrackIndex == 0) {
                mRtcEngine.setAudioMixingDualMonoMode(Constants.AudioMixingDualMonoMode.getValue(Constants.AudioMixingDualMonoMode.AUDIO_MIXING_DUAL_MONO_L));
            } else {
                mRtcEngine.setAudioMixingDualMonoMode(Constants.AudioMixingDualMonoMode.getValue(Constants.AudioMixingDualMonoMode.AUDIO_MIXING_DUAL_MONO_R));
            }
        }
    }

    public void seek(long time) {
        mRtcEngine.setAudioMixingPosition((int) time);
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
        mRtcEngine.adjustAudioMixingVolume(v);
    }

    public void setMicVolume(int v) {
        mRtcEngine.adjustRecordingSignalVolume(v);
    }

    private void startDisplayLrc() {
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

    private void startSyncLrc() {
        mSyncLrcThread = new Thread(new Runnable() {

            @Override
            public void run() {
                DataStreamConfig cfg = new DataStreamConfig();
                cfg.syncWithAudio = false;
                cfg.ordered = false;
                mStreamId = mRtcEngine.createDataStream(cfg);

                mStopSyncLrc = false;
                while (!mStopSyncLrc) {
                    User user = UserManager.Instance().getUserLiveData().getValue();
                    if (user==null) return;
                    if (mStatus == Status.Started && mMusicModel!=null && mMusicModel.getUserId().equals(user.getObjectId())) {
                        mRecvedPlayPosition = mRtcEngine.getAudioMixingCurrentPosition();
                        mLastRecvPlayPosTime = System.currentTimeMillis();

                        sendSyncLrc(mMusicModel.getMusicId(), mRtcEngine.getAudioMixingDuration(), mRecvedPlayPosition,mMusicModel!=null);

                    }
                    AgoraMember member = RoomManager.Instance(mContext).getMine();
                    if (member != null) {
                        sendSyncMember(member.getUserId(), member.getRole(), member.getUser().getAvatar());
                    }

                    try {
                        Thread.sleep(999L);
                    } catch (InterruptedException exp) {
                        break;
                    }
                }
            }
        });
        mSyncLrcThread.setName("Thread-SyncLrc");
        mSyncLrcThread.start();
    }
    public void sendSyncLrc(String lrcId, long duration, long time,boolean shouldPlay) {
        mLogger.d("sendSyncLrc"+lrcId);
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "setLrcTime");
        msg.put("lrcId", lrcId);
        msg.put("duration", duration);
        msg.put("time", time);//ms
        msg.put("state", shouldPlay ? 1 : 0);
        JSONObject jsonMsg = new JSONObject(msg);
        mRtcEngine.sendStreamMessage(mStreamId, jsonMsg.toString().getBytes());
    }

    public void sendSyncMember(String userId, AgoraMember.Role role,String avatar) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "syncMember");
        msg.put("userId", userId);
        msg.put("role", role.getValue());
        msg.put("avatar", avatar);
        JSONObject jsonMsg = new JSONObject(msg);
        mRtcEngine.sendStreamMessage(mStreamId, jsonMsg.toString().getBytes());
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

    public void startPublish() {
        startSyncLrc();
    }

    private void stopPublish() {
        stopSyncLrc();
    }

    private void initAudioTracks() {
        mAudioTrackIndex = 1;
//        mAudioTracksCount = mRtcEngine.getAudioTrackCount();
        mAudioTracksCount = 2;
    }

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
        JSONObject jsonMsg;
        try {
            String strMsg = new String(data);
            jsonMsg = new JSONObject(strMsg);

            if (mStatus.value < Status.Started.value) return;
            String cmd = jsonMsg.getString("cmd");
            if (cmd.equals("setLrcTime") && mMusicModel!=null) {
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

    @Override
    public void onAudioMixingStateChanged(int state, int errorCode) {
        super.onAudioMixingStateChanged(state, errorCode);
        mLogger.d("onAudioMixingStateChanged() called with: state = [%s], errorCode = [%s]", state, errorCode);
        if (state == Constants.MEDIA_ENGINE_AUDIO_EVENT_MIXING_PLAY) {
            if (mStatus == Status.IDLE) {
                onMusicOpenCompleted();
            }
            onMusicPlaying();
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

        initAudioTracks();

        startDisplayLrc();
        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENCOMPLETED, mRtcEngine.getAudioMixingDuration()).sendToTarget();
    }

    private void onMusicOpenError(int error) {
        mLogger.i("onMusicOpenError() called with: error = [%s]", error);
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENERROR, error).sendToTarget();
    }

    protected void onMusicPlayingByListener() {
        mLogger.i("onMusicPlayingByListener() called");
        mStatus = Status.Started;

        mHandler.obtainMessage(ACTION_ON_MUSIC_PLAYING).sendToTarget();
    }

    private void onMusicPlaying() {
        mLogger.i("onMusicPlaying() called");
        mStatus = Status.Started;

//        if (mStopSyncLrc)
//            startPublish();

        mHandler.obtainMessage(ACTION_ON_MUSIC_PLAYING).sendToTarget();
    }

    private void onMusicPause() {
        mLogger.i("onMusicPause() called");
        mStatus = Status.Paused;

        mHandler.obtainMessage(ACTION_ON_MUSIC_PAUSE).sendToTarget();
    }

    private void onMusicStop() {
        mLogger.i("onMusicStop() called");
        mStatus = Status.Stopped;

//        stopDisplayLrc();
//        stopPublish();
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_STOP).sendToTarget();
    }

    private void onMusicCompleted() {
        mLogger.i("onMusicCompleted() called");
//        stopDisplayLrc();
//        stopPublish();
        reset();

        mHandler.obtainMessage(ACTION_ON_MUSIC_COMPLETED).sendToTarget();
    }

    public void destory() {
        stopDisplayLrc();
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
        void onMusicPlaying();

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
}
