package com.agora.data.provider;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.R;

import cn.leancloud.AVLogger;
import cn.leancloud.AVOSCloud;
import cn.leancloud.push.PushService;
import io.agora.baselibrary.BuildConfig;

public class BaseDataProvider implements IDataProvider {

    protected IConfigSource mIConfigSource;
    protected IStoreSource mIStoreSource;
    protected IMessageSource mIMessageSource;

    private Context mContext;
    private IRoomProxy iRoomProxy;

    public BaseDataProvider(@NonNull Context mContext, @NonNull IRoomProxy iRoomProxy) {
        this.mContext = mContext;
        this.iRoomProxy = iRoomProxy;

        if (BuildConfig.DEBUG) {
            AVOSCloud.setLogLevel(AVLogger.Level.DEBUG);
        } else {
            AVOSCloud.setLogLevel(AVLogger.Level.ERROR);
        }

        String appid = mContext.getString(R.string.leancloud_app_id);
        String appKey = mContext.getString(R.string.leancloud_app_key);
        String url = mContext.getString(R.string.leancloud_server_url);
        if (TextUtils.isEmpty(appid) || TextUtils.isEmpty(appKey) || TextUtils.isEmpty(url)) {
            throw new NullPointerException("please check \"strings_config.xml\"");
        }

        AVOSCloud.initialize(mContext, appid, appKey, url);

        PushService.startIfRequired(mContext);

        initConfigSource();
        initStoreSource();
        initMessageSource();
    }

    @Override
    public void initConfigSource() {
        mIConfigSource = new DefaultConfigSource();
    }

    @Override
    public IConfigSource getConfigSource() {
        return mIConfigSource;
    }

    @Override
    public void initStoreSource() {
        mIStoreSource = new StoreSource(mIConfigSource);
    }

    @Override
    public IStoreSource getStoreSource() {
        return mIStoreSource;
    }

    @Override
    public void initMessageSource() {
        mIMessageSource = new MessageSource(mContext, iRoomProxy, mIConfigSource);
    }

    @Override
    public IMessageSource getMessageSource() {
        return mIMessageSource;
    }
}
