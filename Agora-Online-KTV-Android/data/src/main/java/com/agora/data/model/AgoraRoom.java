package com.agora.data.model;

import java.io.Serializable;

public class AgoraRoom implements Serializable {
    public static final String TABLE_NAME = "AgoraRoom";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_OWNERID = "ownerId";
    public static final String COLUMN_NAME = "name";

    private String id;
    private String ownerId;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
