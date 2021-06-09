package com.agora.data.sync;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class CollectionReference {

    private String key;

    private RoomReference parent;
    private DocumentReference mDocumentReference;
    private Query mQuery;

    public CollectionReference(RoomReference parent, String key) {
        this.parent = parent;
        this.key = key;
    }

    public CollectionReference query(Query mQuery) {
        this.mQuery = mQuery;
        return this;
    }

    public Query getQuery() {
        return mQuery;
    }

    public RoomReference getParent() {
        return parent;
    }

    public String getKey() {
        return key;
    }

    public DocumentReference document(@NonNull String id) {
        mDocumentReference = new DocumentReference(this, id);
        return mDocumentReference;
    }

    private HashMap<String, Object> datas = new HashMap<>();

    public HashMap<String, Object> getDatas() {
        return datas;
    }

    public void add(HashMap<String, Object> datas, SyncManager.DataItemCallback callback) {
        this.datas = datas;
        SyncManager.Instance().add(this, datas, callback);
    }

    public void get(SyncManager.DataListCallback callback) {
        SyncManager.Instance().get(this, callback);
    }

    public void delete(SyncManager.Callback callback) {
        SyncManager.Instance().delete(this, callback);
    }
}
