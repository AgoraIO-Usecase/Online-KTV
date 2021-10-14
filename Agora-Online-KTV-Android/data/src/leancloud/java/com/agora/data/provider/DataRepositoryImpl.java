package com.agora.data.provider;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.Config;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.leancloud.LCCloud;
import cn.leancloud.LCObject;
import cn.leancloud.LCQuery;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DataRepositoryImpl implements IDataRepository {

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
    public Observable<List<MusicModel>> getMusics(@Nullable String searchKey) {
        LCQuery<LCObject> mLCQuery = LCQuery.getQuery(MusicModel.TABLE_NAME)
                .limit(500);
        if (TextUtils.isEmpty(searchKey) == false) {
            mLCQuery.whereContains(MusicModel.COLUMN_NAME, searchKey);
        }
        return mLCQuery.findInBackground()
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<List<LCObject>, ObservableSource<List<MusicModel>>>() {
                    @Override
                    public ObservableSource<List<MusicModel>> apply(@NonNull List<LCObject> LCObjects) throws Exception {
                        List<MusicModel> list = new ArrayList<>();
                        for (LCObject object : LCObjects) {
                            MusicModel item = mGson.fromJson(object.toJSONObject().toJSONString(), MusicModel.class);
                            list.add(item);
                        }
                        return Observable.just(list);
                    }
                });
    }

    @Override
    public Observable<MusicModel> getMusic(@NonNull String musicId) {
        Map<String, Object> dicParameters = new HashMap<>();
        dicParameters.put("id", musicId);

        return LCCloud.callFunctionWithCacheInBackground(
                "getMusic",
                dicParameters,
                LCQuery.CachePolicy.CACHE_ELSE_NETWORK,
                30000)
                .flatMap(new Function<Object, ObservableSource<MusicModel>>() {
                    @Override
                    public ObservableSource<MusicModel> apply(@NonNull Object o) throws Exception {
                        return Observable.just(mGson.fromJson(String.valueOf(o), MusicModel.class));
                    }
                });
    }

    private OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    public Completable download(@NonNull File file, @NonNull String url) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@NonNull CompletableEmitter emitter) throws Exception {
                Log.d("down", file.getName() + ", url: " + url);

                if (file.isDirectory()) {
                    emitter.onError(new Throwable("file is a Directory"));
                    return;
                }

                Request request = new Request.Builder().url(url).build();
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        emitter.onError(e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        ResponseBody body = response.body();
                        if (body == null) {
                            emitter.onError(new Throwable("body is empty"));
                            return;
                        }

                        long total = body.contentLength();

                        if (file.exists() && file.length() == total) {
                            emitter.onComplete();
                            return;
                        }

                        InputStream is = null;
                        byte[] buf = new byte[2048];
                        int len = 0;
                        FileOutputStream fos = null;
                        try {
                            is = body.byteStream();
                            fos = new FileOutputStream(file);
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                int progress = (int) (sum * 1.0f / total * 100);
                                Log.d("down", file.getName() + ", progress: " + progress);
                            }
                            fos.flush();
                            // 下载完成
                            Log.d("down", file.getName() + " onComplete");
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        } finally {
                            try {
                                if (is != null)
                                    is.close();
                            } catch (IOException e) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                });
            }
        });
    }
}
