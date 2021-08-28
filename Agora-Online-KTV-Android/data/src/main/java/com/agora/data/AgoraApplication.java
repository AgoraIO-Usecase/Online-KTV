package com.agora.data;

import androidx.multidex.MultiDexApplication;

import com.agora.data.sync.SyncManager;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;

public class AgoraApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        XLog.init(LogLevel.ALL);

        SyncManager.Instance().init(this);
    }
}
