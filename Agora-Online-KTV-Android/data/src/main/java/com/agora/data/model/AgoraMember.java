package com.agora.data.model;

public class AgoraMember {
    private int id;
    private AgoraRoom roomId;
    private int userId;
    private Long streamId = 0L;
    private int isMuted = 0;
    private int isSelfMuted = 0;

    private User user;

    private int role = 2;

    public AgoraMember() {

    }

    public AgoraRoom getRoomId() {
        return roomId;
    }

    public void setRoomId(AgoraRoom roomId) {
        this.roomId = roomId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.userId = user.getUserId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgoraMember that = (AgoraMember) o;

        return id ==that.id;
    }

    @Override
    public int hashCode() {
        int res = 17;
        res = res * 31 + id;
        return res;
    }

    @Override
    public String toString() {
        return "AgoraMember{" +
                "id='" + id + '\'' +
                ", room=" + roomId +
                ", userId='" + userId + '\'' +
                ", streamId=" + streamId +
                ", role=" + role +
                ", isAudioMuted=" + isMuted +
                ", isSelfAudioMuted=" + isSelfMuted +
                '}';
    }
}
