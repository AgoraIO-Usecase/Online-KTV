package com.agora.data.sync;

import com.agora.data.model.AgoraRoom;

import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;

public interface ISyncManager {
    Observable<AgoraRoom> creatRoom(AgoraRoom room);

    Observable<List<AgoraRoom>> getRooms();

    void get(DocumentReference reference, SyncManager.DataItemCallback callback);

    void get(CollectionReference reference, SyncManager.DataListCallback callback);

    void add(CollectionReference reference, HashMap<String, Object> datas, SyncManager.DataItemCallback callback);

    void delete(DocumentReference reference, SyncManager.Callback callback);

    void delete(CollectionReference reference, SyncManager.Callback callback);

    void update(DocumentReference reference, String key, Object data, SyncManager.DataItemCallback callback);

    void update(DocumentReference reference, HashMap<String, Object> datas, SyncManager.DataItemCallback callback);

    void subcribe(DocumentReference reference, SyncManager.EventListener listener);

    void unsubcribe(DocumentReference reference, SyncManager.EventListener listener);
}