package io.agora.ktv.manager;

import android.content.Context;
import android.util.Log;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import io.agora.lrcview.LrcView;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.mediaplayer.IMediaPlayerObserver;
import io.agora.mediaplayer.data.MediaStreamInfo;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;

public class MusicPlayer extends IRtcEngineEventHandler implements IMediaPlayerObserver {
    private static final String TAG = MusicPlayer.class.getSimpleName();

    private Logger.Builder mLogger = XLog.tag("MusicPlayer");

    private Context mContext;
    private RtcEngine mRtcEngine;
    private LrcView mLrcView;
    private int mRole;

    private boolean mStopSyncLrc = true;
    private Thread mSyncLrcThread;

    private boolean mStopDisplayLrc = true;
    private Thread mDisplayThread;
    private String mLrcId = "";

    private IMediaPlayer mPlayer;
    private boolean mIsPlaying = false;
    private long mFreshLrcInterval = 50;
    private long mSyncLrcInterval = 1000;
    private boolean mIsPaused = false;

    private long mRecvedPlayPosition = 0;
    private long mLastRecvPlayPosTime = 0;

    private int mAudioTracksCount = 0;
    private int[] mAudioTrackIndices = null;

    public MusicPlayer(Context mContext, RtcEngine mRtcEngine, LrcView lrcView) {
        this.mContext = mContext;
        this.mRtcEngine = mRtcEngine;
        this.mLrcView = lrcView;

        // init mpk
        mIsPlaying = false;
        mPlayer = mRtcEngine.createMediaPlayer();

        mPlayer.registerPlayerObserver(this);

        mRtcEngine.muteAllRemoteAudioStreams(false);
        mRtcEngine.addHandler(this);
    }

    public void switchRole(int role) {
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

    public void play(String lrc) {
        if (mRole == Constants.CLIENT_ROLE_AUDIENCE) {
            Log.e(TAG, "play: current role is audience, abort playing");
            return;
        }
        if (mIsPlaying) {
            Log.e(TAG, "play: current player is in playing state already, abort playing");
            return;
        }
        if (!mStopDisplayLrc) {
            Log.e(TAG, "play: current player is recving remote streams, abort playing");
            return;
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

        // mpk open file
        mAudioTracksCount = 0;
        mAudioTrackIndices = null;
        mPlayer.open(lrc, 0);
    }

    public void stop() {
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
    }

    public void pause() {
        if (!mIsPlaying)
            return;
        mPlayer.pause();
    }

    public void resume() {
        if (!mIsPlaying)
            return;
        mPlayer.resume();
    }

    public int getAudioTracksCount() {
        return mAudioTracksCount;
    }

    private int mAudioTrackIndex = 0;

    public int getAudioTrackIndex() {
        return mAudioTrackIndex;
    }

    public void selectAudioTrack(int i) {
        if (i < 0 || mAudioTracksCount == 0 || i >= mAudioTracksCount)
            return;

        mAudioTrackIndex = i;
        mPlayer.selectAudioTrack(mAudioTrackIndices[i]);
    }

    public void setMusicVolume(int v) {
        mPlayer.adjustPlayoutVolume(v);
    }

    public void setMicVolume(int v) {
        mRtcEngine.adjustRecordingSignalVolume(v);
    }

    private void startDisplayLrc(String lrcId, long totalDuration) {
        //TODO: generate lrc url by lrcId.
        // As an example, load a const lrc file bellow
        Log.i(TAG, "startDisplayLrc: " + lrcId);
        mLrcId = lrcId;
        String mainLrcText = getLrcText("qinghuaci.lrc");
        String secondLrcText = null; //getLrcText("send_it_cn.lrc");
        mLrcView.post(new Runnable() {
            @Override
            public void run() {
                mLrcView.setTotalDuration(totalDuration);
                mLrcView.loadLrc(mainLrcText, secondLrcText);
            }
        });

        mStopDisplayLrc = false;
        mDisplayThread = new Thread(new Runnable() {
            long mDisplayTime = 0;

            @Override
            public void run() {
                long curTs = 0;
                long curTime = 0;
                while (!mStopDisplayLrc) {
                    curTime = System.currentTimeMillis();
                    if ((curTime - mLastRecvPlayPosTime) <= mSyncLrcInterval) {
                        curTs = mRecvedPlayPosition + (curTime - mLastRecvPlayPosTime);
                    }

                    final long updateTs = curTs;
                    mLrcView.post(new Runnable() {
                        @Override
                        public void run() {
                            mLrcView.updateTime(updateTs);
                        }
                    });
                    try {
                        Thread.sleep(mFreshLrcInterval);
                    } catch (InterruptedException exp) {
                        break;
                    }
                }
                Log.i(TAG, "stoppedDisplayLrc: " + lrcId);
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
                Log.e(TAG, "stopDisplayLrc: " + exp.getMessage());
            }
        }
        mLrcView.post(new Runnable() {
            @Override
            public void run() {
                mLrcView.reset();
            }
        });
        mLrcId = "";
    }

    private String getLrcText(String fileName) {
        String lrcText = null;
        try {
            InputStream is = mContext.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            lrcText = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lrcText;
    }

    private void startSyncLrc(String lrcId, long duration) {
        mSyncLrcThread = new Thread(new Runnable() {
            int mStreamId = -1;
            String curLrcId = "";
            long lrcDuration = 0;

            @Override
            public void run() {
                curLrcId = lrcId;
                lrcDuration = duration;

                Log.i(TAG, "startSyncLrc: " + curLrcId);
                // DataStreamConfig cfg = new DataStreamConfig();
                // cfg.syncWithAudio = false;
                // cfg.ordered = true;
                mStreamId = mRtcEngine.createDataStream(false, false);

                mStopSyncLrc = false;
                while (!mStopSyncLrc && mIsPlaying) {
                    if (!mIsPaused)
                        sendSyncLrc(curLrcId, lrcDuration, mPlayer.getPlayPosition() * 1000);
                    // time += mSyncLrcInterval;
                    try {
                        Thread.sleep(mSyncLrcInterval);
                    } catch (InterruptedException exp) {
                        break;
                    }
                }
                sendMusicStop(curLrcId);
                Log.i(TAG, "stoppedSyncLrc: " + curLrcId);
            }

            private void sendSyncLrc(String lrcId, long duration, long time) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("cmd", "setLrcTime");
                msg.put("lrcId", lrcId);
                msg.put("duration", duration);
                msg.put("time", time); // in ms
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
                Log.e(TAG, "stopSyncLrc: " + exp.getMessage());
            }
        }
    }

    private void startPublish() {
        startSyncLrc("1", mPlayer.getDuration() * 1000);
    }

    private void stopPublish() {
        stopSyncLrc();
    }

    private void initAudioTracks() {
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

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
        //TODO: parse message type
        JSONObject jsonMsg;
        try {
            String strMsg = new String(data);
            jsonMsg = new JSONObject(strMsg);
            Log.i(TAG, "onStreamMessage: recv msg: " + strMsg);
            if (mIsPlaying)
                return;

            if (jsonMsg.getString("cmd").equals("setLrcTime")) {
                if (!jsonMsg.getString("lrcId").equals(mLrcId)) {
                    stopDisplayLrc();
                    // 加载歌词文本
                    mLrcId = jsonMsg.getString("lrcId");
                    startDisplayLrc(mLrcId, jsonMsg.getLong("duration"));
                }
                // update time
                // mLrcView.updateTime();
                mRecvedPlayPosition = jsonMsg.getLong("time");
                mLastRecvPlayPosTime = System.currentTimeMillis();
            } else if (jsonMsg.getString("cmd").equals("musicStopped")) {
                stopDisplayLrc();
            }
        } catch (JSONException exp) {
            Log.e(TAG, "onStreamMessage: failed parse json, error: " + exp.toString());
        }
    }

    @Override
    public void onPlayerStateChanged(io.agora.mediaplayer.Constants.MediaPlayerState state, io.agora.mediaplayer.Constants.MediaPlayerError error) {
        Log.i(TAG, "onPlayerStateChanged: " + state + ", error: " + error);
        // mPlayingState = true;
        try {
            switch (state) {
                case PLAYER_STATE_OPEN_COMPLETED:
                    mIsPlaying = true;
                    stopDisplayLrc();
                    initAudioTracks();
                    mPlayer.play();
                    startDisplayLrc("1", mPlayer.getDuration() * 1000);
                    break;
                case PLAYER_STATE_PLAYING:
                    mIsPaused = false;
                    if (mStopSyncLrc)
                        startPublish();
                    break;
                case PLAYER_STATE_PAUSED:
                    mIsPaused = true;
                    break;
                case PLAYER_STATE_STOPPED:
                case PLAYER_STATE_IDLE:
                case PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED:
                    stopDisplayLrc();
                    stopPublish();
                    mIsPlaying = false;
                    break;
                case PLAYER_STATE_FAILED:
                    mIsPlaying = false;
                    Log.e(TAG, "onPlayerStateChanged: failed to play, error " + error);
                    break;
                default:
            }
        } catch (Exception e) {
            Log.e(TAG, "onPlayerStateChanged: exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onPositionChanged(long position) {
        Log.d(TAG, "onPositionChanged: position: " + position + ", duration: " + mPlayer.getDuration());
        mRecvedPlayPosition = position * 1000;
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

    }
}
