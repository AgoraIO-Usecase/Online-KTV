package io.agora.ktv;

import android.app.Application;
import android.util.Log;

import com.agora.data.sync.SyncManager;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;

import io.agora.rtc2.RtcEngine;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        XLog.init(LogLevel.ALL);
        SyncManager.Instance().init(this);
        SyncManager.setConverter(new MyGsonConverter());
        Log.d("APP", "SDK Version: " + RtcEngine.getSdkVersion());
    }
}