package com.agora.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;

import com.agora.data.ExampleData;
import com.agora.data.R;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class AgoraRoom implements Parcelable {
    public static final String TABLE_NAME = "AGORA_ROOM";

    public static final String COLUMN_ID = "objectId";
    public static final String COLUMN_OWNERID = "userId";
    public static final String COLUMN_NAME = "channelName";
    public static final String COLUMN_COVER = "cover";
    public static final String COLUMN_MV = "mv";
    public static final String COLUMN_CREATEDAT = "createdAt";

    private String id;
    private String channelName;
    private String userId;
    private String cover;
    private String mv;
    private Date createdAt;

    public AgoraRoom() {
    }

    protected AgoraRoom(Parcel in) {
        id = in.readString();
        channelName = in.readString();
        userId = in.readString();
        cover = in.readString();
        mv = in.readString();
        createdAt = (Date) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(channelName);
        dest.writeString(userId);
        dest.writeString(cover);
        dest.writeString(mv);
        dest.writeSerializable(createdAt);
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> datas = new HashMap<>();
        datas.put(COLUMN_OWNERID, userId);
        datas.put(COLUMN_NAME, channelName);
        datas.put(COLUMN_COVER, cover);
        datas.put(COLUMN_MV, mv);
        return datas;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getMv() {
        return mv;
    }

    public void setMv(String mv) {
        this.mv = mv;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void radomCover() {
        int value = new Random().nextInt(8) + 1;
        cover = String.valueOf(value);
    }

    public void radomMV() {
        int value = new Random().nextInt(5) + 1;
        mv = String.valueOf(value);
    }

    @DrawableRes
    public int getCoverRes() {
        int index = 0;
        try {
            index = Integer.parseInt(cover);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if(index >= ExampleData.exampleCovers.size()|| index < 0) index = 0;
        return ExampleData.exampleCovers.get(index);
    }

    @DrawableRes
    public int getMVRes() {
        int index = 0;
        try {
            index = Integer.parseInt(cover);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if(index >= ExampleData.exampleBackgrounds.size()|| index < 0) index = 0;
        return ExampleData.exampleBackgrounds.get(index);
    }

    @Override
    public String toString() {
        return "AgoraRoom{" +
                "id='" + id + '\'' +
                ", channelName='" + channelName + '\'' +
                ", userId='" + userId + '\'' +
                ", cover='" + cover + '\'' +
                ", mv='" + mv + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgoraRoom agoraRoom = (AgoraRoom) o;

        return id.equals(agoraRoom.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
