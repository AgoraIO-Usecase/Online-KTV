package com.agora.data.provider;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/5/25
 */
public class AgoraObject implements IAgoraObject {

    private final DocumentSnapshot document;

    public AgoraObject(DocumentSnapshot document) {
        this.document = document;
    }

    @Override
    public <T> T toObject(@NonNull Class<T> valueType) {
        return document.toObject(valueType);
    }
}
