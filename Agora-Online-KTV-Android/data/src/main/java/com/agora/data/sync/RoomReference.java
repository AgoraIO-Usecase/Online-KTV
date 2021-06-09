package com.agora.data.sync;

import androidx.annotation.NonNull;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class RoomReference extends DocumentReference {

    private CollectionReference mCollectionReference;

    public RoomReference(String id) {
        super(null, id);
    }

    public CollectionReference collection(@NonNull String collectionKey) {
        mCollectionReference = new CollectionReference(this, collectionKey);
        return mCollectionReference;
    }
}
