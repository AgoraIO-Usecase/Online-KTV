package com.agora.data.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/9
 */
public class MusicModel implements Parcelable {
    public static final String TABLE_NAME = "MUSIC_REPOSITORT";

    private String objectId;
    private String musicId;
    private String name;
    private String createdAt;
    private String updatedAt;
    private String song;
    private String lrc;

    protected MusicModel(Parcel in) {
        objectId = in.readString();
        musicId = in.readString();
        name = in.readString();
        createdAt = in.readString();
        updatedAt = in.readString();
        song = in.readString();
        lrc = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(objectId);
        dest.writeString(musicId);
        dest.writeString(name);
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
        dest.writeString(song);
        dest.writeString(lrc);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MusicModel> CREATOR = new Creator<MusicModel>() {
        @Override
        public MusicModel createFromParcel(Parcel in) {
            return new MusicModel(in);
        }

        @Override
        public MusicModel[] newArray(int size) {
            return new MusicModel[size];
        }
    };

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(String musicId) {
        this.musicId = musicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getLrc() {
        return lrc;
    }

    public void setLrc(String lrc) {
        this.lrc = lrc;
    }
}
