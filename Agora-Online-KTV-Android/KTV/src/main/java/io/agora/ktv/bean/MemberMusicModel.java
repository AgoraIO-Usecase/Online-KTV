package io.agora.ktv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.sync.DocumentReference;
import com.agora.data.sync.SyncManager;

import java.io.File;
import java.util.HashMap;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/9
 */
public class MemberMusicModel implements Parcelable {
    public static final String TABLE_NAME = "MUSIC_KTV";

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_USERID = "userId";
    public static final String COLUMN_ROOMID = "roomId";
    public static final String COLUMN_MUSICID = "musicId";
    public static final String COLUMN_CREATE = "createdAt";

    private String id;
    private String name;
    private String userId;
    private AgoraRoom roomId;
    private String musicId;

    private String song;
    private String lrc;

    private File fileMusic;
    private File fileLrc;

    public MemberMusicModel(MusicModel data) {
        this.name = data.getName();
        this.musicId = data.getMusicId();
    }


    protected MemberMusicModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        userId = in.readString();
        roomId = in.readParcelable(AgoraRoom.class.getClassLoader());
        musicId = in.readString();
        song = in.readString();
        lrc = in.readString();
        fileMusic = (File) in.readSerializable();
        fileLrc = (File) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(userId);
        dest.writeParcelable(roomId, flags);
        dest.writeString(musicId);
        dest.writeString(song);
        dest.writeString(lrc);
        dest.writeSerializable(fileMusic);
        dest.writeSerializable(fileLrc);
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

    public void setLocalFile(File fileMusic, File fileLrc) {
        this.fileMusic = fileMusic;
        this.fileLrc = fileLrc;
    }

    public HashMap<String, Object> toHashMap() {
        DocumentReference drRoom = SyncManager.Instance()
                .collection(AgoraRoom.TABLE_NAME)
                .document(roomId.getId());

        HashMap<String, Object> datas = new HashMap<>();
        datas.put(COLUMN_NAME, name);
        datas.put(COLUMN_USERID, userId);
        datas.put(COLUMN_ROOMID, drRoom);
        datas.put(COLUMN_MUSICID, musicId);
        return datas;
    }

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

    public AgoraRoom getRoomId() {
        return roomId;
    }

    public void setRoomId(AgoraRoom roomId) {
        this.roomId = roomId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(String musicId) {
        this.musicId = musicId;
    }

    public File getFileMusic() {
        return fileMusic;
    }

    public void setFileMusic(File fileMusic) {
        this.fileMusic = fileMusic;
    }

    public File getFileLrc() {
        return fileLrc;
    }

    public void setFileLrc(File fileLrc) {
        this.fileLrc = fileLrc;
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
                ", userId='" + userId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", musicId='" + musicId + '\'' +
                '}';
    }

}
