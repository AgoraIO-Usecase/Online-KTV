package io.agora.ktv;

import android.app.Application;
import android.util.Log;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.rtc2.RtcEngine;
import io.reactivex.plugins.RxJavaPlugins;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RxJavaPlugins.setErrorHandler(KTVUtil::logE);
        Log.d("APP", "SDK Version: " + RtcEngine.getSdkVersion());
    }
}