package io.agora.ktvkit;

import io.agora.rtc.IRtcEngineEventHandler;

public class IKTVKitEventHandler {

    private IRtcEngineEventHandler mRTCHandler;

    public IKTVKitEventHandler() {
        mRTCHandler = null;
    }

    public IKTVKitEventHandler(IRtcEngineEventHandler handler) {
        mRTCHandler = handler;
    }

    IRtcEngineEventHandler getRtcEventHandler() {
        return mRTCHandler;
    }

    public void onAudioTrackChanged(int track) {
    }

    public void onPlayerPrepared() {
    }

    public void onPlayerStopped() {

    }

    public void onCurrentPosition(int position) {

    }

    public void onPlayerError(int error) {
    }
}
