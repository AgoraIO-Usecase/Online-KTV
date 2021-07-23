package io.agora.ktv;

import android.util.Log;

import com.agora.data.AgoraApplication;
import com.agora.data.sync.SyncManager;

import io.agora.rtc2.RtcEngine;

public class AppApplication extends AgoraApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        SyncManager.setConverter(new MyGsonConverter());
        Log.d("APP", "SDK Version: " + RtcEngine.getSdkVersion());
    }
}