package io.agora.ktv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;

import java.io.File;
import java.io.Serializable;

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
    public static final String COLUMN_USERBGID = "userbgId";
    public static final String COLUMN_ROOMID = "roomId";
    public static final String COLUMN_MUSICID = "musicId";
    public static final String COLUMN_CREATE = "createdAt";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_USERSTATUS = "userStatus";
    public static final String COLUMN_USER1ID = "user1Id";
    public static final String COLUMN_USER1BGID = "user1bgId";
    public static final String COLUMN_USER1STATUS = "user1Status";
    public static final String COLUMN_APPLYUSERID = "applyUser1Id";

    public enum Type implements Serializable {
        Default, MiGu;
    }

    public enum SingType implements Serializable {
        Single(0), Chorus(1);

        public int value;

        SingType(int value) {
            this.value = value;
        }

        public static SingType parse(int value) {
            if (value == 0) {
                return SingType.Single;
            } else if (value == 1) {
                return SingType.Chorus;
            }
            return SingType.Single;
        }
    }

    public enum UserStatus implements Serializable {
        Idle(0), Ready(1);

        public int value;

        UserStatus(int value) {
            this.value = value;
        }

        public static UserStatus parse(int value) {
            if (value == 0) {
                return UserStatus.Idle;
            } else if (value == 1) {
                return UserStatus.Ready;
            }
            return UserStatus.Idle;
        }
    }

    private String id;
    private String name;

    private AgoraRoom roomId;
    private String musicId;
    private String singer;
    private String poster;
    private String song;
    private String lrc;

    private File fileMusic;
    private File fileLrc;

    private Type musicType = Type.MiGu;

    private SingType type = SingType.Single;

    private String userId;
    private Long userbgId;
    private UserStatus userStatus;

    private String user1Id;
    private Long user1bgId;
    private UserStatus user1Status;

    private String applyUser1Id;

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
        musicType = (Type) in.readSerializable();
        type = (SingType) in.readSerializable();
        userId = in.readString();
        userbgId = in.readLong();
        userStatus = (UserStatus) in.readSerializable();
        user1Id = in.readString();
        user1bgId = in.readLong();
        user1Status = (UserStatus) in.readSerializable();
        applyUser1Id = in.readString();
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
        dest.writeSerializable(musicType);
        dest.writeSerializable(type);
        dest.writeString(userId);
        dest.writeLong(userbgId);
        dest.writeSerializable(userStatus);
        dest.writeString(user1Id);
        dest.writeLong(user1bgId);
        dest.writeSerializable(user1Status);
        dest.writeString(applyUser1Id);
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

    public Type getMusicType() {
        return musicType;
    }

    public void setMusicType(Type musicType) {
        this.musicType = musicType;
    }

    public SingType getType() {
        if (type == null) {
            type = SingType.Single;
        }
        return type;
    }

    public void setType(SingType type) {
        this.type = type;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }

    public UserStatus getUser1Status() {
        return user1Status;
    }

    public void setUser1Status(UserStatus user1Status) {
        this.user1Status = user1Status;
    }

    public String getApplyUser1Id() {
        return applyUser1Id;
    }

    public void setApplyUser1Id(String applyUser1Id) {
        this.applyUser1Id = applyUser1Id;
    }

    public Long getUserbgId() {
        return userbgId;
    }

    public void setUserbgId(Long userbgId) {
        this.userbgId = userbgId;
    }

    public Long getUser1bgId() {
        return user1bgId;
    }

    public void setUser1bgId(Long user1bgId) {
        this.user1bgId = user1bgId;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public void setPropertiesWithMusic(MusicModel music){
        this.musicId = music.getMusicId();
        this.name = music.getName();
        this.singer = music.getSinger();
        this.poster = music.getPoster();
        this.lrc = music.getLrc();
        this.song = music.getSong();
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
                ", roomId=" + roomId +
                ", musicId='" + musicId + '\'' +
                ", singer='" + singer + '\'' +
                ", poster='" + poster + '\'' +
                ", song='" + song + '\'' +
                ", lrc='" + lrc + '\'' +
                ", fileMusic=" + fileMusic +
                ", fileLrc=" + fileLrc +
                ", musicType=" + musicType +
                ", type=" + type +
                ", userId='" + userId + '\'' +
                ", userbgId=" + userbgId +
                ", userStatus=" + userStatus +
                ", user1Id='" + user1Id + '\'' +
                ", user1bgId=" + user1bgId +
                ", user1Status=" + user1Status +
                ", applyUser1Id='" + applyUser1Id + '\'' +
                '}';
    }
}
