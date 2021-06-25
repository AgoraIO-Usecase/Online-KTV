package com.agora.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.agora.data.R;

import java.util.HashMap;
import java.util.Random;

public class AgoraRoom implements Parcelable {
    public static final String TABLE_NAME = "AGORA_ROOM";

    public static final String COLUMN_ID = "id";
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

    public AgoraRoom() {
    }

    protected AgoraRoom(Parcel in) {
        id = in.readString();
        channelName = in.readString();
        userId = in.readString();
        cover = in.readString();
        mv = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(channelName);
        dest.writeString(userId);
        dest.writeString(cover);
        dest.writeString(mv);
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

    public void radomCover() {
        int value = new Random().nextInt(8) + 1;
        cover = String.valueOf(value);
    }

    public void radomMV() {
        int value = new Random().nextInt(5) + 1;
        mv = String.valueOf(value);
    }

    public int getCoverRes() {
        if ("1".equals(cover)) {
            return R.mipmap.icon_room_cover1;
        } else if ("2".equals(cover)) {
            return R.mipmap.icon_room_cover2;
        } else if ("3".equals(cover)) {
            return R.mipmap.icon_room_cover3;
        } else if ("4".equals(cover)) {
            return R.mipmap.icon_room_cover4;
        } else if ("5".equals(cover)) {
            return R.mipmap.icon_room_cover5;
        } else if ("6".equals(cover)) {
            return R.mipmap.icon_room_cover6;
        } else if ("7".equals(cover)) {
            return R.mipmap.icon_room_cover7;
        } else if ("8".equals(cover)) {
            return R.mipmap.icon_room_cover8;
        } else if ("9".equals(cover)) {
            return R.mipmap.icon_room_cover9;
        }
        return R.mipmap.icon_room_cover1;
    }

    public int getMVRes() {
        if ("1".equals(mv)) {
            return R.mipmap.ktv_music_background1;
        } else if ("2".equals(mv)) {
            return R.mipmap.ktv_music_background2;
        } else if ("3".equals(mv)) {
            return R.mipmap.ktv_music_background3;
        } else if ("4".equals(mv)) {
            return R.mipmap.ktv_music_background4;
        } else if ("5".equals(mv)) {
            return R.mipmap.ktv_music_background5;
        } else if ("6".equals(mv)) {
            return R.mipmap.ktv_music_background6;
        } else if ("7".equals(mv)) {
            return R.mipmap.ktv_music_background7;
        } else if ("8".equals(mv)) {
            return R.mipmap.ktv_music_background8;
        } else if ("9".equals(mv)) {
            return R.mipmap.ktv_music_background9;
        }
        return R.mipmap.ktv_music_background1;
    }

    @Override
    public String toString() {
        return "AgoraRoom{" +
                "id='" + id + '\'' +
                ", name='" + channelName + '\'' +
                ", ownerId='" + userId + '\'' +
                ", cover='" + cover + '\'' +
                ", mv='" + mv + '\'' +
                '}';
    }
}
