package com.agora.data.provider;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.agora.data.EnumActionSerializer;
import com.agora.data.EnumRoleSerializer;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import cn.leancloud.AVException;
import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
import cn.leancloud.livequery.AVLiveQuery;
import cn.leancloud.livequery.AVLiveQueryEventHandler;
import cn.leancloud.livequery.AVLiveQuerySubscribeCallback;

public class AVLiveQueryHelp<T> {

    public static final int ERROR_DEFAULT = 1000;
    public static final int ERROR_EXCEEDED_QUOTA = ERROR_DEFAULT + 1;

    public static final String TAG = AVLiveQueryHelp.class.getSimpleName();

    private AVLiveQuery avLiveQuery;

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Member.Role.class, new EnumRoleSerializer())
            .registerTypeAdapter(Action.ACTION.class, new EnumActionSerializer())
            .create();

    public interface AttributeListener<T> {
        void onCreated(T item);

        void onUpdated(T item);

        void onDeleted(String objectId);

        void onSubscribeError(int error);
    }

    private final Handler mHandler = new Handler(Looper.myLooper());
    private Runnable runnable;

    private final Class<T> mCLass;

    public AVLiveQueryHelp(Class<T> mCLass) {
        this.mCLass = mCLass;
    }

    protected T convertObject(AVObject object) {
        return mGson.fromJson(object.toJSONObject().toJSONString(), mCLass);
    }

    private String getTag() {
        return this.mCLass.getSimpleName();
    }

    public void registerObserve(AVQuery<AVObject> query, AttributeListener<T> callback) {
        avLiveQuery = AVLiveQuery.initWithQuery(query);
        avLiveQuery.setEventHandler(new AVLiveQueryEventHandler() {

            @Override
            public void onObjectCreated(AVObject avObject) {
                super.onObjectCreated(avObject);
                Log.d(TAG, String.format("%s onObjectCreated: %s", getTag(), avObject));
                callback.onCreated(convertObject(avObject));
            }

            @Override
            public void onObjectUpdated(AVObject avObject, List<String> updatedKeys) {
                super.onObjectUpdated(avObject, updatedKeys);
                Log.d(TAG, String.format("%s onObjectUpdated: %s", getTag(), avObject));
                callback.onUpdated(convertObject(avObject));
            }

            @Override
            public void onObjectDeleted(String objectId) {
                super.onObjectDeleted(objectId);
                Log.d(TAG, String.format("%s onObjectDeleted: %s", getTag(), objectId));
                callback.onDeleted(objectId);
            }
        });

        //如果同时订阅，会导致前一次订阅无任何回调，所以这里做一个超时监控处理
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, String.format("%s subscribe error: timeout", getTag()));
                callback.onSubscribeError(ERROR_DEFAULT);
            }
        };
        mHandler.postDelayed(runnable, 5000L);
        avLiveQuery.subscribeInBackground(new AVLiveQuerySubscribeCallback() {
            @Override
            public void done(AVException e) {
                if (null != e) {
                    Log.e(TAG, String.format("%s subscribe error: %s", getTag(), e.getMessage()));
                    avLiveQuery = null;
                    if (e.getCode() == AVException.EXCEEDED_QUOTA) {
                        callback.onSubscribeError(ERROR_EXCEEDED_QUOTA);
                    } else {
                        callback.onSubscribeError(ERROR_DEFAULT);
                    }
                } else {
                    Log.i(TAG, String.format("%s subscribe success", getTag()));
                }
                mHandler.removeCallbacks(runnable);
                runnable = null;
            }
        });
    }

    public void unregisterObserve() {
        Log.i(TAG, String.format("%s unregisterObserve", getTag()));

        if (runnable != null) {
            mHandler.removeCallbacks(runnable);
        }
        if (avLiveQuery != null) {
            avLiveQuery.unsubscribeInBackground(new AVLiveQuerySubscribeCallback() {
                @Override
                public void done(AVException e) {
                    if (null != e) {
                        Log.e(TAG, String.format("%s unsubscribe error: %s", getTag(), e.getMessage()));
                    } else {
                        Log.i(TAG, String.format("%s unsubscribe success", getTag()));
                    }
                }
            });
            avLiveQuery = null;
        }
    }
}
