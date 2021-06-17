package com.agora.data.provider;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.Config;
import com.agora.data.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
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
            AVObject avObject = new AVObject(User.TABLE_NAME);
            avObject.put(Config.USER_NAME, user.getName());
            avObject.put(Config.USER_AVATAR, user.getAvatar());
            return avObject.saveInBackground()
                    .subscribeOn(Schedulers.io())
                    .concatMap(new Function<AVObject, ObservableSource<? extends User>>() {
                        @Override
                        public ObservableSource<? extends User> apply(@NonNull AVObject avObject) throws Exception {
                            User user = mGson.fromJson(avObject.toJSONObject().toJSONString(), User.class);
                            return Observable.just(user);
                        }
                    });
        } else {
            AVQuery<AVObject> query = AVQuery.getQuery(User.TABLE_NAME);
            query.whereEqualTo(Config.USER_OBJECTID, user.getObjectId());
            return query.countInBackground()
                    .subscribeOn(Schedulers.io())
                    .concatMap(new Function<Integer, Observable<User>>() {
                        @Override
                        public Observable<User> apply(@NonNull Integer integer) throws Exception {
                            if (integer <= 0) {
                                AVObject avObject = new AVObject(User.TABLE_NAME);
                                avObject.put(Config.USER_NAME, user.getName());
                                avObject.put(Config.USER_AVATAR, user.getAvatar());
                                return avObject.saveInBackground()
                                        .concatMap(new AVObjectToObservable<>(new TypeToken<User>() {
                                        }.getType()));
                            } else {
                                return query.getFirstInBackground()
                                        .concatMap(new Function<AVObject, ObservableSource<? extends User>>() {
                                            @Override
                                            public ObservableSource<? extends User> apply(@NonNull AVObject avObject) throws Exception {
                                                User user = mGson.fromJson(avObject.toJSONObject().toJSONString(), User.class);
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
        AVObject avObject = AVObject.createWithoutData(User.TABLE_NAME, user.getObjectId());
        avObject.put(Config.USER_NAME, user.getName());
        avObject.put(Config.USER_AVATAR, user.getAvatar());
        return avObject.saveInBackground()
                .subscribeOn(Schedulers.io())
                .concatMap(new AVObjectToObservable<>(new TypeToken<User>() {
                }.getType()));
    }

    @Override
    public Observable<User> getUser(@NonNull String userId) {
        return AVQuery.getQuery(User.TABLE_NAME)
                .getInBackground(userId)
                .subscribeOn(Schedulers.io())
                .concatMap(new AVObjectToObservable<>(new TypeToken<User>() {
                }.getType()));
    }
}
