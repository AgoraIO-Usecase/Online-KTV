package com.agora.data.provider;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.Config;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import cn.leancloud.LCObject;
import cn.leancloud.LCQuery;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DataRepositroyImpl implements IDataRepositroy {

    private Gson mGson = new GsonBuilder()
            .create();

    @Override
    public Observable<User> login(@NonNull User user) {
        if (TextUtils.isEmpty(user.getObjectId())) {
            LCObject mLCObject = new LCObject(User.TABLE_NAME);
            mLCObject.put(Config.USER_NAME, user.getName());
            mLCObject.put(Config.USER_AVATAR, user.getAvatar());
            return mLCObject.saveInBackground()
                    .subscribeOn(Schedulers.io())
                    .concatMap(new Function<LCObject, ObservableSource<? extends User>>() {
                        @Override
                        public ObservableSource<? extends User> apply(@NonNull LCObject LCObject) throws Exception {
                            User user = mGson.fromJson(LCObject.toJSONObject().toJSONString(), User.class);
                            return Observable.just(user);
                        }
                    });
        } else {
            LCQuery<LCObject> query = LCQuery.getQuery(User.TABLE_NAME);
            query.whereEqualTo(Config.USER_OBJECTID, user.getObjectId());
            return query.countInBackground()
                    .subscribeOn(Schedulers.io())
                    .concatMap(new Function<Integer, Observable<User>>() {
                        @Override
                        public Observable<User> apply(@NonNull Integer integer) throws Exception {
                            if (integer <= 0) {
                                LCObject LCObject = new LCObject(User.TABLE_NAME);
                                LCObject.put(Config.USER_NAME, user.getName());
                                LCObject.put(Config.USER_AVATAR, user.getAvatar());
                                return LCObject.saveInBackground()
                                        .concatMap(new AVObjectToObservable<>(new TypeToken<User>() {
                                        }.getType()));
                            } else {
                                return query.getFirstInBackground()
                                        .concatMap(new Function<LCObject, ObservableSource<? extends User>>() {
                                            @Override
                                            public ObservableSource<? extends User> apply(@NonNull LCObject LCObject) throws Exception {
                                                User user = mGson.fromJson(LCObject.toJSONObject().toJSONString(), User.class);
                                                return Observable.just(user);
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        LCObject mLCObject = LCObject.createWithoutData(User.TABLE_NAME, user.getObjectId());
        mLCObject.put(Config.USER_NAME, user.getName());
        mLCObject.put(Config.USER_AVATAR, user.getAvatar());
        return mLCObject.saveInBackground()
                .subscribeOn(Schedulers.io())
                .concatMap(new AVObjectToObservable<>(new TypeToken<User>() {
                }.getType()));
    }

    @Override
    public Observable<User> getUser(@NonNull String userId) {
        return LCQuery.getQuery(User.TABLE_NAME)
                .getInBackground(userId)
                .subscribeOn(Schedulers.io())
                .concatMap(new AVObjectToObservable<>(new TypeToken<User>() {
                }.getType()));
    }

    @Override
    public Observable<List<MusicModel>> getMusics() {
        return LCQuery.getQuery(MusicModel.TABLE_NAME)
                .findInBackground()
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<List<LCObject>, ObservableSource<List<MusicModel>>>() {
                    @Override
                    public ObservableSource<List<MusicModel>> apply(@NonNull List<LCObject> LCObjects) throws Exception {
                        List<MusicModel> list = new ArrayList<>();
                        for (LCObject LCObject : LCObjects) {
                            MusicModel item = mGson.fromJson(LCObject.toJSONString(), MusicModel.class);
                            list.add(item);
                        }
                        return Observable.just(list);
                    }
                });
    }
}
