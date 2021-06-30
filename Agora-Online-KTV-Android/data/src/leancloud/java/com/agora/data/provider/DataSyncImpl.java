package com.agora.data.provider;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.R;
import com.agora.data.model.AgoraRoom;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.CollectionReference;
import com.agora.data.sync.DocumentReference;
import com.agora.data.sync.FieldFilter;
import com.agora.data.sync.ISyncManager;
import com.agora.data.sync.OrderBy;
import com.agora.data.sync.Query;
import com.agora.data.sync.RoomReference;
import com.agora.data.sync.SyncManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.leancloud.LCException;
import cn.leancloud.LCLogger;
import cn.leancloud.LCObject;
import cn.leancloud.LCQuery;
import cn.leancloud.LeanCloud;
import cn.leancloud.livequery.LCLiveQuery;
import cn.leancloud.livequery.LCLiveQueryEventHandler;
import cn.leancloud.livequery.LCLiveQuerySubscribeCallback;
import cn.leancloud.push.PushService;
import cn.leancloud.types.LCNull;
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
            LeanCloud.setLogLevel(LCLogger.Level.DEBUG);
        } else {
            LeanCloud.setLogLevel(LCLogger.Level.ERROR);
        }

        String appid = mContext.getString(R.string.leancloud_app_id);
        String appKey = mContext.getString(R.string.leancloud_app_key);
        String url = mContext.getString(R.string.leancloud_server_url);
        if (TextUtils.isEmpty(appid) || TextUtils.isEmpty(appKey) || TextUtils.isEmpty(url)) {
            throw new NullPointerException("please check \"strings_config.xml\"");
        }

        LeanCloud.initialize(mContext, appid, appKey, url);

        PushService.startIfRequired(mContext);
    }

    @Override
    public Observable<AgoraRoom> creatRoom(AgoraRoom room) {
        LCObject mLCObject = new LCObject(AgoraRoom.TABLE_NAME);
        Map<String, Object> datas = room.toHashMap();
        for (Map.Entry<String, Object> entry : datas.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            mLCObject.put(key, value);
        }
        return mLCObject.saveInBackground()
                .subscribeOn(Schedulers.io())
                .map(new Function<LCObject, AgoraRoom>() {
                    @Override
                    public AgoraRoom apply(@NonNull LCObject LCObject) throws Exception {
                        AgoraRoom mAgoraRoom = mGson.fromJson(LCObject.toJSONObject().toJSONString(), AgoraRoom.class);
                        mAgoraRoom.setId(LCObject.getObjectId());
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
        LCQuery<LCObject> mLCQuery = LCQuery.getQuery(AgoraRoom.TABLE_NAME);
        mLCQuery.limit(30);
        mLCQuery.orderByDescending(AgoraRoom.COLUMN_CREATEDAT);
        return mLCQuery.findInBackground()
                .subscribeOn(Schedulers.io())
                .map(new Function<List<LCObject>, List<AgoraRoom>>() {
                    @Override
                    public List<AgoraRoom> apply(@NonNull List<LCObject> LCObjects) throws Exception {
                        List<AgoraRoom> rooms = new ArrayList<>();
                        for (LCObject object : LCObjects) {
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
            LCQuery<LCObject> mLCQuery = LCQuery.getQuery(AgoraRoom.TABLE_NAME);
            mLCQuery.getInBackground(reference.getId())
                    .subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCObject LCObject) {
                            callback.onSuccess(new AgoraObject(LCObject));
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
            LCQuery<LCObject> mLCQuery = LCQuery.getQuery(collectionKey);
            mLCQuery.getInBackground(reference.getId())
                    .subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCObject LCObject) {
                            callback.onSuccess(new AgoraObject(LCObject));
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
        LCQuery<LCObject> mLCQuery = createLCQuery(reference.getKey(), reference.getQuery());
        mLCQuery.findInBackground()
                .subscribe(new Observer<List<LCObject>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<LCObject> LCObjects) {
                        List<AgoraObject> list = new ArrayList<>();
                        for (LCObject LCObject : LCObjects) {
                            list.add(new AgoraObject(LCObject));
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
        LCObject mLCObject = new LCObject(collectionKey);
        for (Map.Entry<String, Object> entry : datas.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof DocumentReference) {
                CollectionReference referenceParent = ((DocumentReference) value).getParent();
                LCObject LCObjectItem = mLCObject.createWithoutData(referenceParent.getKey(), ((DocumentReference) value).getId());
                mLCObject.put(key, LCObjectItem);
            } else {
                mLCObject.put(key, value);
            }
        }
        mLCObject.saveInBackground()
                .subscribe(new Observer<LCObject>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull LCObject LCObject) {
                        callback.onSuccess(new AgoraObject(LCObject));
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
            LCObject mLCObject = LCObject.createWithoutData(AgoraRoom.TABLE_NAME, reference.getId());
            mLCObject.deleteInBackground()
                    .subscribe(new Observer<LCNull>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCNull LCNull) {
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
            LCObject mLCObjectCollection = LCObject.createWithoutData(collectionKey, reference.getId());
            mLCObjectCollection.deleteInBackground()
                    .subscribe(new Observer<LCNull>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCNull LCNull) {
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

    private LCQuery<LCObject> createLCQuery(String theClassName, Query mQuery) {
        LCQuery<LCObject> mLCQuery = LCQuery.getQuery(theClassName);

        if (mQuery != null) {
            List<FieldFilter> list = mQuery.getFilters();
            for (FieldFilter filter : list) {
                if (filter.getOperator() == FieldFilter.Operator.EQUAL) {
                    String field = filter.getField();
                    Object value = filter.getValue();
                    if (value instanceof DocumentReference) {
                        CollectionReference referenceParent = ((DocumentReference) value).getParent();
                        LCObject LCObjectItem = LCObject.createWithoutData(referenceParent.getKey(), ((DocumentReference) value).getId());
                        mLCQuery.whereEqualTo(field, LCObjectItem);
                    } else {
                        mLCQuery.whereEqualTo(field, value);
                    }
                }
            }

            List<OrderBy> orderByList = mQuery.getOrderByList();
            for (OrderBy item : orderByList) {
                if (item.getDirection() == OrderBy.Direction.ASCENDING) {
                    mLCQuery.addAscendingOrder(item.getField());
                } else if (item.getDirection() == OrderBy.Direction.DESCENDING) {
                    mLCQuery.addDescendingOrder(item.getField());
                }
            }
        }

        return mLCQuery;
    }

    @Override
    public void delete(CollectionReference reference, SyncManager.Callback callback) {
        String collectionKey = reference.getKey();
        Query mQuery = reference.getQuery();
        LCQuery<LCObject> mLCQuery = createLCQuery(collectionKey, mQuery);
        mLCQuery.deleteAllInBackground()
                .subscribe(new Observer<LCNull>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull LCNull LCNull) {
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
            LCObject mLCQuery = LCObject.createWithoutData(AgoraRoom.TABLE_NAME, reference.getId());
            mLCQuery.put(key, data);
            mLCQuery.saveInBackground()
                    .subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCObject LCObject) {
                            callback.onSuccess(new AgoraObject(LCObject));
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
            LCObject mLCObjectCollection = LCObject.createWithoutData(collectionKey, reference.getId());
            mLCObjectCollection.put(key, data);
            mLCObjectCollection.saveInBackground()
                    .subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCObject LCObject) {
                            callback.onSuccess(new AgoraObject(LCObject));
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
            LCObject mLCObject = LCObject.createWithoutData(AgoraRoom.TABLE_NAME, reference.getId());
            for (Map.Entry<String, Object> entry : datas.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof DocumentReference) {
                    CollectionReference referenceParent = ((DocumentReference) value).getParent();
                    LCObject LCObjectItem = mLCObject.createWithoutData(referenceParent.getKey(), ((DocumentReference) value).getId());
                    mLCObject.put(key, LCObjectItem);
                } else {
                    mLCObject.put(key, value);
                }
            }
            mLCObject.saveInBackground()
                    .subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCObject LCObject) {
                            callback.onSuccess(new AgoraObject(LCObject));
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
            LCObject mLCObjectCollection = LCObject.createWithoutData(collectionKey, reference.getId());
            for (Map.Entry<String, Object> entry : datas.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof DocumentReference) {
                    CollectionReference referenceParent = ((DocumentReference) value).getParent();
                    LCObject LCObjectItem = LCObject.createWithoutData(referenceParent.getKey(), ((DocumentReference) value).getId());
                    mLCObjectCollection.put(key, LCObjectItem);
                } else {
                    mLCObjectCollection.put(key, value);
                }
            }
            mLCObjectCollection.saveInBackground()
                    .subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull LCObject LCObject) {
                            callback.onSuccess(new AgoraObject(LCObject));
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

    private HashMap<SyncManager.EventListener, LCLiveQuery> events = new HashMap<>();

    @Override
    public void subcribe(DocumentReference reference, SyncManager.EventListener listener) {
        if (reference instanceof RoomReference) {
            LCQuery<LCObject> query = createLCQuery(AgoraRoom.TABLE_NAME, reference.getQuery());
            LCLiveQuery mLCLiveQuery = LCLiveQuery.initWithQuery(query);
            mLCLiveQuery.setEventHandler(new LCLiveQueryEventHandler() {

                @Override
                public void onObjectCreated(LCObject LCObject) {
                    super.onObjectCreated(LCObject);
                    listener.onCreated(new AgoraObject(LCObject));
                }

                @Override
                public void onObjectUpdated(LCObject LCObject, List<String> updatedKeys) {
                    super.onObjectUpdated(LCObject, updatedKeys);
                    listener.onUpdated(new AgoraObject(LCObject));
                }

                @Override
                public void onObjectDeleted(String objectId) {
                    super.onObjectDeleted(objectId);
                    listener.onDeleted(objectId);
                }
            });

            events.put(listener, mLCLiveQuery);
            mLCLiveQuery.subscribeInBackground(new LCLiveQuerySubscribeCallback() {
                @Override
                public void done(LCException e) {
                    if (null != e) {
                        if (e.getCode() == LCException.EXCEEDED_QUOTA) {
                            listener.onSubscribeError(new AgoraException(AgoraException.ERROR_LEANCLOULD_OVER_COUNT, e.getMessage()));
                        } else {
                            listener.onSubscribeError(new AgoraException(AgoraException.ERROR_LEANCLOULD_DEFAULT, e.getMessage()));
                        }
                    } else {
                    }
                }
            });
        } else {
            String collectionKey = reference.getParent().getKey();
            LCQuery<LCObject> query = createLCQuery(collectionKey, reference.getQuery());
            LCLiveQuery mLCLiveQuery = LCLiveQuery.initWithQuery(query);
            mLCLiveQuery.setEventHandler(new LCLiveQueryEventHandler() {

                @Override
                public void onObjectCreated(LCObject LCObject) {
                    super.onObjectCreated(LCObject);
                    listener.onCreated(new AgoraObject(LCObject));
                }

                @Override
                public void onObjectUpdated(LCObject LCObject, List<String> updatedKeys) {
                    super.onObjectUpdated(LCObject, updatedKeys);
                    listener.onUpdated(new AgoraObject(LCObject));
                }

                @Override
                public void onObjectDeleted(String objectId) {
                    super.onObjectDeleted(objectId);
                    listener.onDeleted(objectId);
                }
            });

            events.put(listener, mLCLiveQuery);
            mLCLiveQuery.subscribeInBackground(new LCLiveQuerySubscribeCallback() {
                @Override
                public void done(LCException e) {
                    if (null != e) {
                        if (e.getCode() == LCException.EXCEEDED_QUOTA) {
                            listener.onSubscribeError(new AgoraException(AgoraException.ERROR_LEANCLOULD_OVER_COUNT, e.getMessage()));
                        } else {
                            listener.onSubscribeError(new AgoraException(AgoraException.ERROR_LEANCLOULD_DEFAULT, e.getMessage()));
                        }
                    } else {
                    }
                }
            });
        }
    }

    @Override
    public void subcribe(CollectionReference reference, SyncManager.EventListener listener) {
        String collectionKey = reference.getKey();
        LCQuery<LCObject> query = createLCQuery(collectionKey, reference.getQuery());
        LCLiveQuery mLCLiveQuery = LCLiveQuery.initWithQuery(query);
        mLCLiveQuery.setEventHandler(new LCLiveQueryEventHandler() {

            @Override
            public void onObjectCreated(LCObject LCObject) {
                super.onObjectCreated(LCObject);
                listener.onCreated(new AgoraObject(LCObject));
            }

            @Override
            public void onObjectUpdated(LCObject LCObject, List<String> updatedKeys) {
                super.onObjectUpdated(LCObject, updatedKeys);
                listener.onUpdated(new AgoraObject(LCObject));
            }

            @Override
            public void onObjectDeleted(String objectId) {
                super.onObjectDeleted(objectId);
                listener.onDeleted(objectId);
            }
        });

        events.put(listener, mLCLiveQuery);
        mLCLiveQuery.subscribeInBackground(new LCLiveQuerySubscribeCallback() {
            @Override
            public void done(LCException e) {
                if (null != e) {
                    if (e.getCode() == LCException.EXCEEDED_QUOTA) {
                        listener.onSubscribeError(new AgoraException(AgoraException.ERROR_LEANCLOULD_OVER_COUNT, e.getMessage()));
                    } else {
                        listener.onSubscribeError(new AgoraException(AgoraException.ERROR_LEANCLOULD_DEFAULT, e.getMessage()));
                    }
                } else {

                }
            }
        });
    }

    @Override
    public void unsubcribe(SyncManager.EventListener listener) {
        if (events.get(listener) != null) {
            events.get(listener).unsubscribeInBackground(new LCLiveQuerySubscribeCallback() {
                @Override
                public void done(LCException e) {

                }
            });
        }
    }
}
