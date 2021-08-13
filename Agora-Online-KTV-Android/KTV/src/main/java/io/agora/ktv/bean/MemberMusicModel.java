package io.agora.ktv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.sync.DocumentReference;
import com.agora.data.sync.SyncManager;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/9
 */
public class MemberMusicModel implements Parcelable {
    public static final String TABLE_NAME = "MUSIC_KTV";

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SINGER = "singer";
    public static final String COLUMN_POSTER = "poster";
    public static final String COLUMN_USERID = "userId";
    public static final String COLUMN_ROOMID = "roomId";
    public static final String COLUMN_MUSICID = "musicId";
    public static final String COLUMN_CREATE = "createdAt";

    public enum Type implements Serializable {
        Default, MiGu;
    }

    private String id;
    private String name;
    private String singer;
    private String poster;
    private String userId;
    private AgoraRoom roomId;
    private String musicId;

    private String song;
    private String lrc;

    private File fileMusic;
    private File fileLrc;

    private Type type = Type.MiGu;

    public MemberMusicModel(String musicId) {
        this.musicId = musicId;
    }

    public MemberMusicModel(MusicModel data) {
        this.name = data.getName();
        this.musicId = data.getMusicId();
        this.singer = data.getSinger();
        this.poster = data.getPoster();
    }

    protected MemberMusicModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        roomId = in.readParcelable(AgoraRoom.class.getClassLoader());
        musicId = in.readString();
        song = in.readString();
        singer = in.readString();
        poster = in.readString();
        lrc = in.readString();
        fileMusic = (File) in.readSerializable();
        fileLrc = (File) in.readSerializable();
        type = (Type) in.readSerializable();
        userId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeParcelable(roomId, flags);
        dest.writeString(musicId);
        dest.writeString(song);
        dest.writeString(singer);
        dest.writeString(poster);
        dest.writeString(lrc);
        dest.writeSerializable(fileMusic);
        dest.writeSerializable(fileLrc);
        dest.writeSerializable(type);
        dest.writeString(userId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MemberMusicModel> CREATOR = new Creator<MemberMusicModel>() {
        @Override
        public MemberMusicModel createFromParcel(Parcel in) {
            return new MemberMusicModel(in);
        }

        @Override
        public MemberMusicModel[] newArray(int size) {
            return new MemberMusicModel[size];
        }
    };

    public HashMap<String, Object> toHashMap() {
        DocumentReference drRoom = SyncManager.Instance()
                .collection(AgoraRoom.TABLE_NAME)
                .document(roomId.getId());

        HashMap<String, Object> datas = new HashMap<>();
        datas.put(COLUMN_NAME, name);
        datas.put(COLUMN_SINGER, singer);
        datas.put(COLUMN_POSTER, poster);
        datas.put(COLUMN_ROOMID, drRoom);
        datas.put(COLUMN_MUSICID, musicId);
        datas.put(COLUMN_USERID, userId);
        return datas;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemberMusicModel that = (MemberMusicModel) o;

        if (id == null || id.isEmpty()) {
            return musicId.equals(that.musicId);
        }

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MemberMusicModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", singer='" + singer + '\'' +
                ", poster='" + poster + '\'' +
                ", userId='" + userId + '\'' +
                ", roomId=" + roomId +
                ", musicId='" + musicId + '\'' +
                ", song='" + song + '\'' +
                ", lrc='" + lrc + '\'' +
                ", fileMusic=" + fileMusic +
                ", fileLrc=" + fileLrc +
                ", type=" + type +
                '}';
    }
}
