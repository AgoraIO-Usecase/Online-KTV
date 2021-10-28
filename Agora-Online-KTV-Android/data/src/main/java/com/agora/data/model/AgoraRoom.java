package com.agora.data.model;

import java.io.Serializable;
import java.util.Date;

public class AgoraRoom implements Serializable {

    private String id;
    private String channelName;
    private String userId;
    private String cover;
    private String mv;
    private Date createdAt;

    public AgoraRoom() {
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
