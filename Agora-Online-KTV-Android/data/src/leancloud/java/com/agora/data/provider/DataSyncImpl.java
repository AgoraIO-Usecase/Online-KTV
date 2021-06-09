package com.agora.data.provider;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.Config;
import com.agora.data.R;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.CollectionReference;
import com.agora.data.sync.DocumentReference;
import com.agora.data.sync.FieldFilter;
import com.agora.data.sync.ISyncManager;
import com.agora.data.sync.Query;
import com.agora.data.sync.RoomReference;
import com.agora.data.sync.SyncManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.leancloud.AVException;
import cn.leancloud.AVLogger;
import cn.leancloud.AVOSCloud;
import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
import cn.leancloud.livequery.AVLiveQuery;
import cn.leancloud.livequery.AVLiveQueryEventHandler;
import cn.leancloud.livequery.AVLiveQuerySubscribeCallback;
import cn.leancloud.push.PushService;
import cn.leancloud.types.AVNull;
import io.agora.baselibrary.BuildConfig;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DataSyncImpl implements ISyncManager {

    private Gson mGson = new Gson();

    public DataSyncImpl(Context mContext) {
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
    }

    @Override
    public Observable<AgoraRoom> creatRoom(AgoraRoom room) {
        AVObject avObject = new AVObject(AgoraRoom.TABLE_NAME);
        avObject.put(AgoraRoom.COLUMN_OWNERID, room.getOwnerId());
        avObject.put(AgoraRoom.COLUMN_NAME, room.getName());
        return avObject.saveInBackground()
                .subscribeOn(Schedulers.io())
                .map(new Function<AVObject, AgoraRoom>() {
                    @Override
                    public AgoraRoom apply(@NonNull AVObject avObject) throws Exception {
                        AgoraRoom mAgoraRoom = mGson.fromJson(avObject.toJSONObject().toJSONString(), AgoraRoom.class);
                        mAgoraRoom.setId(avObject.getObjectId());
                        return mAgoraRoom;
                    }
                }).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends AgoraRoom>>() {
                    @Override
                    public ObservableSource<? extends AgoraRoom> apply(@NonNull Throwable throwable) throws Exception {
                        return Observable.error(new AgoraException(throwable));
                    }
                });
    }

    @Override
    public Observable<List<AgoraRoom>> getRooms() {
        AVQuery<AVObject> query = AVQuery.getQuery(AgoraRoom.TABLE_NAME);
        query.limit(30);
        query.orderByDescending(Config.ROOM_CREATEDAT);
        return query.findInBackground()
                .subscribeOn(Schedulers.io())
                .map(new Function<List<AVObject>, List<AgoraRoom>>() {
                    @Override
                    public List<AgoraRoom> apply(@NonNull List<AVObject> avObjects) throws Exception {
                        List<AgoraRoom> rooms = new ArrayList<>();
                        for (AVObject object : avObjects) {
                            AgoraRoom room = mGson.fromJson(object.toJSONObject().toJSONString(), AgoraRoom.class);
                            room.setId(object.getObjectId());
                            rooms.add(room);
                        }
                        return rooms;
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends List<AgoraRoom>>>() {
                    @Override
                    public ObservableSource<? extends List<AgoraRoom>> apply(@NonNull Throwable throwable) throws Exception {
                        return Observable.error(new AgoraException(throwable));
                    }
                });
    }

    @Override
    public void get(DocumentReference reference, SyncManager.DataItemCallback callback) {
        if (reference instanceof RoomReference) {
            AVQuery<AVObject> query = AVQuery.getQuery(AgoraRoom.TABLE_NAME);
            query.getInBackground(reference.getId())
                    .subscribe(new Observer<AVObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVObject avObject) {
                            callback.onSuccess(new AgoraObject(avObject));
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            String collectionKey = reference.getParent().getKey();
            AVQuery<AVObject> avQuery = AVQuery.getQuery(collectionKey);
            avQuery.getInBackground(reference.getId())
                    .subscribe(new Observer<AVObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVObject avObject) {
                            callback.onSuccess(new AgoraObject(avObject));
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    @Override
    public void get(CollectionReference reference, SyncManager.DataListCallback callback) {
        String roomId = reference.getParent().getId();
        AVQuery<AVObject> avQuery = AVQuery.getQuery(reference.getKey());
        avQuery.whereEqualTo(AgoraMember.COLUMN_ROOMID, roomId);
        avQuery.findInBackground()
                .subscribe(new Observer<List<AVObject>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<AVObject> avObjects) {
                        List<AgoraObject> list = new ArrayList<>();
                        for (AVObject avObject : avObjects) {
                            list.add(new AgoraObject(avObject));
                        }
                        callback.onSuccess(list);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        callback.onFail(new AgoraException(e));
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void add(CollectionReference reference, HashMap<String, Object> datas, SyncManager.DataItemCallback callback) {
        String collectionKey = reference.getKey();
        AVObject avObject = new AVObject(collectionKey);
        for (Map.Entry<String, Object> entry : datas.entrySet()) {
            avObject.put(entry.getKey(), entry.getValue());
        }
        avObject.saveInBackground()
                .subscribe(new Observer<AVObject>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull AVObject avObject) {
                        callback.onSuccess(new AgoraObject(avObject));
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        callback.onFail(new AgoraException(e));
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void delete(DocumentReference reference, SyncManager.Callback callback) {
        if (reference instanceof RoomReference) {
            AVObject avObjectRoom = AVObject.createWithoutData(AgoraRoom.TABLE_NAME, reference.getId());
            AVQuery<AVObject> avQueryMember = AVQuery.getQuery(AgoraMember.TABLE_NAME)
                    .whereEqualTo(AgoraMember.COLUMN_ROOMID, reference.getId());

            Observable.concat(avQueryMember.deleteAllInBackground(), avObjectRoom.deleteInBackground())
                    .subscribe(new Observer<AVNull>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVNull avNull) {
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            String collectionKey = reference.getParent().getKey();
            AVObject avObjectcollection = AVObject.createWithoutData(collectionKey, reference.getId());
            avObjectcollection.deleteInBackground()
                    .subscribe(new Observer<AVNull>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVNull avNull) {
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private AVQuery<AVObject> createAVQuery(String theClassName, Query mQuery) {
        AVQuery<AVObject> mAVQuery = AVQuery.getQuery(theClassName);

        if (mQuery != null) {
            List<FieldFilter> list = mQuery.getFilters();
            for (FieldFilter filter : list) {
                if (filter.getOperator() == FieldFilter.Operator.EQUAL) {
                    mAVQuery.whereEqualTo(filter.getField(), filter.getValue());
                }
            }
        }

        return mAVQuery;
    }

    @Override
    public void delete(CollectionReference reference, SyncManager.Callback callback) {
        String collectionKey = reference.getKey();
        Query mQuery = reference.getQuery();
        AVQuery<AVObject> mAVQuery = createAVQuery(collectionKey, mQuery);
        mAVQuery.deleteAllInBackground()
                .subscribe(new Observer<AVNull>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull AVNull avNull) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        callback.onFail(new AgoraException(e));
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void update(DocumentReference reference, String key, Object data, SyncManager.DataItemCallback callback) {
        if (reference instanceof RoomReference) {
            AVObject avObject = AVObject.createWithoutData(AgoraRoom.TABLE_NAME, reference.getId());
            avObject.put(key, data);
            avObject.saveInBackground()
                    .subscribe(new Observer<AVObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVObject avObject) {
                            callback.onSuccess(new AgoraObject(avObject));
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            String collectionKey = reference.getParent().getKey();
            AVObject avObjectCollection = AVObject.createWithoutData(collectionKey, reference.getId());
            avObjectCollection.put(key, data);
            avObjectCollection.saveInBackground()
                    .subscribe(new Observer<AVObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVObject avObject) {
                            callback.onSuccess(new AgoraObject(avObject));
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    @Override
    public void update(DocumentReference reference, HashMap<String, Object> datas, SyncManager.DataItemCallback callback) {
        if (reference instanceof RoomReference) {
            AVObject avObject = AVObject.createWithoutData(AgoraRoom.TABLE_NAME, reference.getId());
            for (Map.Entry<String, Object> entry : datas.entrySet()) {
                avObject.put(entry.getKey(), entry.getValue());
            }
            avObject.saveInBackground()
                    .subscribe(new Observer<AVObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVObject avObject) {
                            callback.onSuccess(new AgoraObject(avObject));
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            String collectionKey = reference.getParent().getKey();
            AVObject avObjectCollection = AVObject.createWithoutData(collectionKey, reference.getId());
            for (Map.Entry<String, Object> entry : datas.entrySet()) {
                avObjectCollection.put(entry.getKey(), entry.getValue());
            }
            avObjectCollection.saveInBackground()
                    .subscribe(new Observer<AVObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull AVObject avObject) {
                            callback.onSuccess(new AgoraObject(avObject));
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            callback.onFail(new AgoraException(e));
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private HashMap<SyncManager.EventListener, AVLiveQuery> events = new HashMap<>();

    @Override
    public void subcribe(DocumentReference reference, SyncManager.EventListener listener) {
        if (reference instanceof RoomReference) {
            AVQuery<AVObject> query = createAVQuery(AgoraRoom.TABLE_NAME, reference.getQuery());
            AVLiveQuery avLiveQuery = AVLiveQuery.initWithQuery(query);
            avLiveQuery.setEventHandler(new AVLiveQueryEventHandler() {

                @Override
                public void onObjectCreated(AVObject avObject) {
                    super.onObjectCreated(avObject);
                    listener.onCreated(new AgoraObject(avObject));
                }

                @Override
                public void onObjectUpdated(AVObject avObject, List<String> updatedKeys) {
                    super.onObjectUpdated(avObject, updatedKeys);
                    listener.onUpdated(new AgoraObject(avObject));
                }

                @Override
                public void onObjectDeleted(String objectId) {
                    super.onObjectDeleted(objectId);
                    listener.onDeleted(objectId);
                }
            });

            events.put(listener, avLiveQuery);
            avLiveQuery.subscribeInBackground(new AVLiveQuerySubscribeCallback() {
                @Override
                public void done(AVException e) {
                    if (null != e) {
                        listener.onSubscribeError(1);
                    } else {
                    }
                }
            });
        }
    }

    @Override
    public void unsubcribe(DocumentReference reference, SyncManager.EventListener listener) {
        if (events.get(listener) != null) {
            events.get(listener).unsubscribeInBackground(new AVLiveQuerySubscribeCallback() {
                @Override
                public void done(AVException e) {

                }
            });
        }
    }
}
