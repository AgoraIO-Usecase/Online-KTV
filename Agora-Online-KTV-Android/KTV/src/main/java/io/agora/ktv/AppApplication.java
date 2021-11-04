package io.agora.ktv;

import android.app.Application;
import android.util.Log;

import com.agora.data.sync.SyncManager;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.rtc2.RtcEngine;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        XLog.init(LogLevel.ALL);
        SyncManager.Instance().init(this);
        SyncManager.setConverter(new MyGsonConverter());
        RxJavaPlugins.setErrorHandler(KTVUtil::logE);
        Log.d("APP", "SDK Version: " + RtcEngine.getSdkVersion());
    }
}