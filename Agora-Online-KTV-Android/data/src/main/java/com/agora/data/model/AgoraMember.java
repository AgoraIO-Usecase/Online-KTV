package com.agora.data.model;

import java.util.HashMap;

public class AgoraMember {
    public static final String TABLE_NAME = "AgoraMember";

    public static final String COLUMN_ROOMID = "roomId";
    public static final String COLUMN_STREAMID = "streamId";
    public static final String COLUMN_USERID = "userId";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_ISAUDIOMUTED = "isAudioMuted";
    public static final String COLUMN_ISSELFAUDIOMUTED = "isSelfAudioMuted";

    private String id;
    private String roomId;
    private String userId;
    private Long streamId;
    private int role = 0;
    private int isAudioMuted = 0;
    private int isSelfAudioMuted = 0;

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> datas = new HashMap<>();
        datas.put(COLUMN_ROOMID, roomId);
        datas.put(COLUMN_STREAMID, streamId);
        datas.put(COLUMN_USERID, userId);
        datas.put(COLUMN_ROLE, role);
        datas.put(COLUMN_ISAUDIOMUTED, isAudioMuted);
        datas.put(COLUMN_ISSELFAUDIOMUTED, isSelfAudioMuted);
        return datas;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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
}
