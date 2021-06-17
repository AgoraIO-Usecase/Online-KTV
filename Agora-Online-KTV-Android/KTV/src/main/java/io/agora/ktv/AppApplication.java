package io.agora.ktv;

import android.util.Log;

import com.agora.data.AgoraApplication;

import io.agora.rtc2.RtcEngine;

public class AppApplication extends AgoraApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("APP", "SDK Vserion: " + RtcEngine.getSdkVersion());
    }
}