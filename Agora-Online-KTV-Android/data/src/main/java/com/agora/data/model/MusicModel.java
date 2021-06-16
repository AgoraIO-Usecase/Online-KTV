package com.agora.data.model;

import java.util.HashMap;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/9
 */
public class MusicModel {
    public static final String TABLE_NAME = "AgoraMusic";

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_USERID = "userId";
    public static final String COLUMN_ROOMID = "roomId";
    public static final String COLUMN_MUSICID = "musicId";

    private String id;
    private String name;
    private String userId;
    private String roomId;
    private String musicId;

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> datas = new HashMap<>();
        datas.put(COLUMN_NAME, name);
        datas.put(COLUMN_USERID, userId);
        datas.put(COLUMN_ROOMID, roomId);
        datas.put(COLUMN_MUSICID, musicId);
        return datas;
    }

    public MusicModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(String musicId) {
        this.musicId = musicId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MusicModel that = (MusicModel) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
