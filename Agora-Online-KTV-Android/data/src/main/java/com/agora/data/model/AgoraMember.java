package com.agora.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.agora.data.sync.DocumentReference;
import com.agora.data.sync.SyncManager;

import java.util.HashMap;

public class AgoraMember implements Parcelable {
    public static final String TABLE_NAME = "AgoraMember";

    public static final String COLUMN_ROOMID = "roomId";
    public static final String COLUMN_STREAMID = "streamId";
    public static final String COLUMN_USERID = "userId";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_ISAUDIOMUTED = "isAudioMuted";
    public static final String COLUMN_ISSELFAUDIOMUTED = "isSelfAudioMuted";

    public enum Role {
        Listener(0), Owner(1), Speaker(2);
        private int value;

        Role(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static AgoraMember.Role parse(int value) {
            if (value == 0) {
                return AgoraMember.Role.Listener;
            } else if (value == 1) {
                return AgoraMember.Role.Owner;
            } else if (value == 2) {
                return AgoraMember.Role.Speaker;
            }
            return AgoraMember.Role.Listener;
        }

    }

    private String id;
    private AgoraRoom room;
    private String userId;
    private Long streamId;
    private Role role = Role.Listener;
    private int isAudioMuted = 0;
    private int isSelfAudioMuted = 0;

    private User user;

    public AgoraMember() {

    }

    protected AgoraMember(Parcel in) {
        id = in.readString();
        room = in.readParcelable(AgoraRoom.class.getClassLoader());
        userId = in.readString();
        if (in.readByte() == 0) {
            streamId = null;
        } else {
            streamId = in.readLong();
        }
        isAudioMuted = in.readInt();
        isSelfAudioMuted = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeParcelable(room, flags);
        dest.writeString(userId);
        if (streamId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(streamId);
        }
        dest.writeInt(isAudioMuted);
        dest.writeInt(isSelfAudioMuted);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AgoraMember> CREATOR = new Creator<AgoraMember>() {
        @Override
        public AgoraMember createFromParcel(Parcel in) {
            return new AgoraMember(in);
        }

        @Override
        public AgoraMember[] newArray(int size) {
            return new AgoraMember[size];
        }
    };

    public HashMap<String, Object> toHashMap() {
        DocumentReference drRoom = SyncManager.Instance()
                .collection(AgoraRoom.TABLE_NAME)
                .document(room.getId());

        HashMap<String, Object> datas = new HashMap<>();
        datas.put(COLUMN_ROOMID, drRoom);
        datas.put(COLUMN_STREAMID, streamId);
        datas.put(COLUMN_USERID, userId);
        datas.put(COLUMN_ROLE, role.value);
        datas.put(COLUMN_ISAUDIOMUTED, isAudioMuted);
        datas.put(COLUMN_ISSELFAUDIOMUTED, isSelfAudioMuted);
        return datas;
    }

    public AgoraRoom getRoom() {
        return room;
    }

    public void setRoom(AgoraRoom room) {
        this.room = room;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getIsAudioMuted() {
        return isAudioMuted;
    }

    public void setIsAudioMuted(int isAudioMuted) {
        this.isAudioMuted = isAudioMuted;
    }

    public int getIsSelfAudioMuted() {
        return isSelfAudioMuted;
    }

    public void setIsSelfAudioMuted(int isSelfAudioMuted) {
        this.isSelfAudioMuted = isSelfAudioMuted;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgoraMember that = (AgoraMember) o;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AgoraMember{" +
                "id='" + id + '\'' +
                ", room=" + room +
                ", userId='" + userId + '\'' +
                ", streamId=" + streamId +
                ", role=" + role +
                ", isAudioMuted=" + isAudioMuted +
                ", isSelfAudioMuted=" + isSelfAudioMuted +
                '}';
    }
}
