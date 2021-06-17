package com.agora.data.sync;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraRoom;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class RoomReference extends DocumentReference {

    private CollectionReference mCollectionReference;

    public RoomReference(String id) {
        super(new CollectionReference(null, AgoraRoom.TABLE_NAME), id);
    }

    public CollectionReference collection(@NonNull String collectionKey) {
        mCollectionReference = new CollectionReference(this, collectionKey);
        return mCollectionReference;
    }
}
