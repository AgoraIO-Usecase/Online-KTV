package com.agora.data.manager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.agora.data.Config;
import com.agora.data.R;
import com.agora.data.provider.IRoomConfigProvider;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.RtcEngineConfig;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

public final class RtcManager {

    private Logger.Builder mLogger = XLog.tag(RtcManager.class.getSimpleName());

    private final String TAG = RtcManager.class.getSimpleName();

    private volatile static RtcManager instance;

    private Context mContext;
    private RtcEngine mRtcEngine;

    public volatile String channel;
    public volatile int uid;
    private boolean isJoined = false;

    private IRoomConfigProvider roomConfig;

    private RtcManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized RtcManager Instance(Context context) {
        if (instance == null) {
            synchronized (RtcManager.class) {
                if (instance == null)
                    instance = new RtcManager(context);
            }
        }
        return instance;
    }

    public void init() {
        mLogger.d("init() called");
        if (mRtcEngine == null) {
            String appid = mContext.getString(R.string.app_id);
            if (TextUtils.isEmpty(appid)) {
                throw new NullPointerException("please check \"strings_config.xml\"");
            }

            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = mContext;
            config.mAppId = appid;
            config.mEventHandler = mEventHandler;
            if (Config.isLeanCloud()) {
                config.mAreaCode = RtcEngineConfig.AreaCode.AREA_CODE_CN;
            } else {
                config.mAreaCode = RtcEngineConfig.AreaCode.AREA_CODE_GLOB;
            }

            try {
                mRtcEngine = RtcEngine.create(config);
            } catch (Exception e) {
                e.printStackTrace();
                mLogger.e("init error", e);
            }
        }
    }

    public void setupRoomConfig(IRoomConfigProvider roomConfig) {
        RtcManager.this.roomConfig = roomConfig;
        roomConfig.setup(getRtcEngine());
    }

    private SingleEmitter<Integer> emitterJoin;

    public Single<Integer> joinChannel(String channelId, int userId) {
        mLogger.d("joinChannel() called with: channelId = [" + channelId + "], userId = [" + userId + "]");

        return Single.create(emitter -> {
            this.emitterJoin = emitter;

            if (mRtcEngine == null) {
                emitter.onError(new NullPointerException("mRtcEngine is null"));
                return;
            }

            if (isJoined) {
                emitter.onSuccess(uid);
                return;
            }
            mRtcEngine.joinChannel(mContext.getString(R.string.token), channelId, null, userId);
        });
    }

    public void leaveChannel() {
        mLogger.d("leaveChannel() called");
        if (mRtcEngine == null) {
            return;
        }

        mRtcEngine.leaveChannel();
        RtcManager.this.roomConfig = null;
    }

    public IRoomConfigProvider getRoomConfig() {
        return roomConfig;
    }

    public RtcEngine getRtcEngine() {
        if (mRtcEngine == null) {
            throw new NullPointerException("mRtcEngine is null");
        }
        return mRtcEngine;
    }

    public void addHandler(IRtcEngineEventHandler handler) {
        if (mRtcEngine == null) {
            return;
        }

        mRtcEngine.addHandler(handler);
    }

    public void removeHandler(IRtcEngineEventHandler handler) {
        if (mRtcEngine == null) {
            return;
        }

        mRtcEngine.removeHandler(handler);
    }

    private final IRtcEngineEventHandler mEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
            Log.w(TAG, "onWarning: " + warn);
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            Log.e(TAG, "onError: " + err + " , " + RtcEngine.getErrorDescription(err));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            mLogger.d("onJoinChannelSuccess() called with: channel = [" + channel + "], uid = [" + uid + "], elapsed = [" + elapsed + "]");
            RtcManager.this.isJoined = true;
            RtcManager.this.channel = channel;
            RtcManager.this.uid = uid;
            RtcManager.this.emitterJoin.onSuccess(uid);
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            mLogger.d("onLeaveChannel() called with: stats = [" + stats + "]");
            RtcManager.this.isJoined = false;
            RtcManager.this.channel = null;
            RtcManager.this.uid = 0;
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            mLogger.d("onUserJoined() called with: uid = [" + uid + "], elapsed = [" + elapsed + "]");
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            mLogger.d("onUserOffline() called with: uid = [" + uid + "], reason = [" + reason + "]");
        }

        @Override
        public void onLocalAudioStateChanged(int state, int error) {
            super.onLocalAudioStateChanged(state, error);
            mLogger.d("onLocalAudioStateChanged() called with: state = [" + state + "], error = [" + error + "]");
        }

        @Override
        public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
            mLogger.d("onRemoteAudioStateChanged() called with: uid = [" + uid + "], state = [" + state + "], reason = [" + reason + "], elapsed = [" + elapsed + "]");
        }
    };
}