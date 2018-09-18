package io.agora.ktvkit;

import android.content.Context;
import android.view.SurfaceHolder;

import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.AgoraVideoFrame;

public class KTVKit {

    private static RtcEngine mRtcEngine;

    private static final IKtvNativeLoader sLocalLibLoader = new IKtvNativeLoader() {
        @Override
        public void loadLibrary(String libName) throws UnsatisfiedLinkError, SecurityException {
            System.loadLibrary(libName);
        }
    };

    private static volatile boolean mIsLibLoaded = false;

    public static void loadLibrariesOnce(IKtvNativeLoader libLoader) {
        synchronized (KTVKit.class) {
            if (!mIsLibLoaded) {
                if (libLoader == null)
                    libLoader = sLocalLibLoader;

                libLoader.loadLibrary("agora-ktv-kit");
                mIsLibLoaded = true;
            }
        }
    }

    private static volatile boolean mIsNativeInitialized = false;

    private static void initNativeOnce() {
        synchronized (KTVKit.class) {
            if (!mIsNativeInitialized) {
                mIsNativeInitialized = true;
            }
        }
    }

    private static KTVKit mKTVKit;

    private IKTVKitEventHandler mKTVHandler;

    private KTVKit() {
    }

    public static KTVKit create(RtcEngine rtcEngine, Context context, IKTVKitEventHandler handler) throws Exception {
        if (rtcEngine == null) {
            throw new RuntimeException("Must have a valid rtc engine");
        }

        if (handler == null) {
            throw new RuntimeException("Must have a valid KTV event handler");
        }

        loadLibrariesOnce(sLocalLibLoader);
        initNativeOnce();

        mRtcEngine = rtcEngine;

        if (mKTVKit == null) {
            mKTVKit = new KTVKit();
        }

        mKTVKit.mKTVHandler = handler;

        mKTVKit.setCallBack();
        mKTVKit.initAgoraObserver();

        return mKTVKit;
    }

    public static KTVKit create(Context context, String appId, IKTVKitEventHandler handler) throws Exception {
        if (handler == null) {
            throw new RuntimeException("Must have a valid KTV event handler");
        }

        loadLibrariesOnce(sLocalLibLoader);
        initNativeOnce();

        mRtcEngine = RtcEngine.create(context, appId, handler.getRtcEventHandler());

        if (mKTVKit == null) {
            mKTVKit = new KTVKit();
        }

        mKTVKit.mKTVHandler = handler;

        mKTVKit.setCallBack();
        mKTVKit.initAgoraObserver();

        return mKTVKit;
    }

    @SuppressWarnings("Unused")
    private void doSendVideoFrameToCloud(final byte[] data, int width, int height) {
        AgoraVideoFrame f = new AgoraVideoFrame();
        f.format = AgoraVideoFrame.FORMAT_I420;
        f.timeStamp = System.currentTimeMillis();
        f.buf = data;
        f.stride = width;
        f.height = height;

        if (mRtcEngine == null) {
            return;
        }

        boolean ret = mRtcEngine.pushExternalVideoFrame(f);
    }

    public void openAndPlayVideoFile(String url) {
        closeSong();
        Open(url);
    }

    public void pause() {
        PlayOrPause();
    }

    public void resume() {
        PlayOrPause();
    }

    public void stopPlayVideoFile() {
        closeSong();
    }

    public void switchAudioTrack() {
        ChangeAudio();
    }

    public void resetAudioBuffer() {
        destroyAudioBuf();
    }

    public void setupDisplay(SurfaceHolder hd) {

    }

    public int getCurrentPosition() {
        return 0;
    }

    public int getDuration() {
        return 0;
    }

    public void seekTo(int seekToMs) {

    }

    public boolean isPlaying() {
        return true;
    }

    public void setupRemoteVideoView() {

    }

    public void adjustVoiceVolume(double volume) {
        voiceSeek(volume);
    }

    public void adjustAccompanyVolume(double volume) {
        songSeek(volume);
    }

    public void enableInEarMonitoring() {

    }

    public void setLocalVoiceReverb() {

    }

    // 初始化音频观察期对象
    private native void initAgoraObserver();

    // 打开播放器
    private native void Open(String url);

    // 暂停播放
    private native void PlayOrPause();

    // 设置视频渲染回调
    private native void setCallBack();

    // 切音轨
    private native void ChangeAudio();

    // 调节人声音量
    private native void voiceSeek(double pos);

    // 调节伴奏音量
    private native void songSeek(double pos);

    // 关闭 mv
    private native void closeSong();

    // 清空音频缓冲区缓存
    private native void destroyAudioBuf();

    // 获取视频进度
    private native double PlayPos();

    // 播放完成之后的回调
    void videoComplete() {
//        log.debug("视频播放完成");
        if (mKTVHandler != null) {
            mKTVHandler.onPlayerStopped();
        }
    }

    // 视频时长回调
    void totalMSCallBack(int time) {
//        log.debug("视频播放时长 %d",time);
        if (mKTVHandler != null) {
            mKTVHandler.onCurrentPosition(time);
        }
    }

    public RtcEngine getRtcEngine() {
        return null;
    }

    public static synchronized void destroy() {
        RtcEngine.destroy();

        if (mRtcEngine != null) {
            mRtcEngine = null;
        }

        if (mKTVKit != null) {
            mKTVKit = null;
        }
    }
}
