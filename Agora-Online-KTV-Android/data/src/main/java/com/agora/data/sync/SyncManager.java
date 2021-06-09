package com.agora.data.sync;

import android.content.Context;

import com.agora.data.model.AgoraRoom;
import com.agora.data.provider.AgoraObject;
import com.agora.data.provider.DataSyncImpl;

import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;

/**
 * 房间状态同步
 */
public final class SyncManager implements ISyncManager {

    private volatile static SyncManager instance;

    private SyncManager() {
    }

    public static SyncManager Instance() {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null)
                    instance = new SyncManager();
            }
        }
        return instance;
    }

    private ISyncManager mISyncManager;

    public void init(Context mContext) {
        mISyncManager = new DataSyncImpl(mContext);
    }

    public RoomReference getRoom(String id) {
        return new RoomReference(id);
    }

    @Override
    public Observable<AgoraRoom> creatRoom(AgoraRoom room) {
        return mISyncManager.creatRoom(room);
    }

    @Override
    public Observable<List<AgoraRoom>> getRooms() {
        return mISyncManager.getRooms();
    }

    @Override
    public void get(DocumentReference reference, SyncManager.DataItemCallback callback) {
        mISyncManager.get(reference, callback);
    }

    @Override
    public void get(CollectionReference reference, SyncManager.DataListCallback callback) {
        mISyncManager.get(reference, callback);
    }

    @Override
    public void add(CollectionReference reference, HashMap<String, Object> datas, SyncManager.DataItemCallback callback) {
        mISyncManager.add(reference, datas, callback);
    }

    @Override
    public void delete(DocumentReference reference, SyncManager.Callback callback) {
        mISyncManager.delete(reference, callback);
    }

    @Override
    public void delete(CollectionReference reference, Callback callback) {
        mISyncManager.delete(reference, callback);
    }

    @Override
    public void update(DocumentReference reference, String key, Object data, SyncManager.DataItemCallback callback) {
        mISyncManager.update(reference, key, data, callback);
    }

    @Override
    public void update(DocumentReference reference, HashMap<String, Object> datas, DataItemCallback callback) {
        mISyncManager.update(reference, datas, callback);
    }

    @Override
    public void subcribe(DocumentReference reference, SyncManager.EventListener listener) {
        mISyncManager.subcribe(reference, listener);
    }

    @Override
    public void unsubcribe(DocumentReference reference, SyncManager.EventListener listener) {
        mISyncManager.unsubcribe(reference, listener);
    }

    public interface EventListener {
        void onCreated(AgoraObject item);

        void onUpdated(AgoraObject item);

        void onDeleted(String objectId);

        void onSubscribeError(int error);
    }

    public interface Callback {
        void onSuccess();

        void onFail(AgoraException exception);
    }

    public interface DataItemCallback {
        void onSuccess(AgoraObject result);

        void onFail(AgoraException exception);
    }

    public interface DataListCallback {
        void onSuccess(List<AgoraObject> result);

        void onFail(AgoraException exception);
    }
}
