package com.agora.data.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Member implements Serializable, Cloneable {
    public static final String TABLE_NAME = "ROOM";

    public enum Role {
        Listener(0), Speaker(1), Left(2), Right(3);

        int value;

        Role(int value) {
            this.value = value;
        }

        public static Role parse(int value) {
            if (value == 0) {
                return Role.Listener;
            } else if (value == 1) {
                return Role.Speaker;
            } else if (value == 2) {
                return Role.Left;
            } else if (value == 3) {
                return Role.Right;
            }
            return Role.Listener;
        }

        public int getValue() {
            return value;
        }
    }

    private String objectId;
    private Room roomId;
    private Long streamId;
    private User userId;

    //是否是演讲者，区分开观众和演讲者。0-不是，1-是。
    private int isSpeaker = 0;
    private Role role = Role.Listener;

    //是否被管理员禁言，0-没，1-被禁言。
    private int isMuted = 0;

    //是否自己禁言，0-没，1-禁言。
    private int isSelfMuted = 0;
    private int isSDKVideoMuted = 0;

    public Member() {
    }

    public Member(@NonNull User mUser) {
        setUser(mUser);
    }

    public void setUser(@NonNull User mUser) {
        setUserId(mUser);
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public Room getRoomId() {
        return roomId;
    }

    public void setRoomId(Room roomId) {
        this.roomId = roomId;
    }

    public Long getStreamId() {
        return streamId;
    }

    public int getStreamIntId() {
        return streamId.intValue();
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public User getUserId() {
        return userId;
    }

    public void setUserId(User userId) {
        this.userId = userId;
    }

    public int getIsSpeaker() {
        return isSpeaker;
    }

    public void setIsSpeaker(int isSpeaker) {
        this.isSpeaker = isSpeaker;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getIsMuted() {
        return isMuted;
    }

    public void setIsMuted(int isMuted) {
        this.isMuted = isMuted;
    }

    public int getIsSelfMuted() {
        return isSelfMuted;
    }

    public void setIsSelfMuted(int isSelfMuted) {
        this.isSelfMuted = isSelfMuted;
    }

    public int getIsSDKVideoMuted() {
        return isSDKVideoMuted;
    }

    public void setIsSDKVideoMuted(int isSDKVideoMuted) {
        this.isSDKVideoMuted = isSDKVideoMuted;
    }

    @Override
    public String toString() {
        return "Member{" +
                "objectId='" + objectId + '\'' +
                ", roomId=" + roomId +
                ", streamId=" + streamId +
                ", userId=" + userId +
                ", isSpeaker=" + isSpeaker +
                ", role=" + role +
                ", isMuted=" + isMuted +
                ", isSelfMuted=" + isSelfMuted +
                ", isSDKVideoMuted=" + isSDKVideoMuted +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member member = (Member) o;

        return objectId.equals(member.objectId);
    }

    @Override
    public int hashCode() {
        return objectId.hashCode();
    }

    @NonNull
    @Override
    public Member clone() {
        try {
            return (Member) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new Member();
    }

}
