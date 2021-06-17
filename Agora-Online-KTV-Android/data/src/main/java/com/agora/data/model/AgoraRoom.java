package com.agora.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class AgoraRoom implements Parcelable {
    public static final String TABLE_NAME = "AgoraRoom";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_OWNERID = "ownerId";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CREATEDAT = "createdAt";

    private String id;
    private String name;
    private String ownerId;

    public AgoraRoom() {
    }

    protected AgoraRoom(Parcel in) {
        id = in.readString();
        name = in.readString();
        ownerId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(ownerId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AgoraRoom> CREATOR = new Creator<AgoraRoom>() {
        @Override
        public AgoraRoom createFromParcel(Parcel in) {
            return new AgoraRoom(in);
        }

        @Override
        public AgoraRoom[] newArray(int size) {
            return new AgoraRoom[size];
        }
    };

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

    public enum MusicStatus {
        IDLE, START, PAUSE
    }

    @Override
    public String toString() {
        return "AgoraRoom{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ownerId='" + ownerId + '\'' +
                '}';
    }
}
