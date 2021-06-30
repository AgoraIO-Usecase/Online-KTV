package io.agora.ktv;

import android.util.Log;

import com.agora.data.AgoraApplication;

import io.agora.rtc.RtcEngine;

public class AppApplication extends AgoraApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("APP", "SDK Version: " + RtcEngine.getSdkVersion());
    }
}