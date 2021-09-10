package com.agora.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExampleData {
    public static final List<String> exampleRoomNames = new ArrayList<>(Arrays.asList(
       "和你一起看月亮",
       "治愈",
       "一锤定音",
       "有酒吗",
       "早安序曲",
       "风情万种的歌房"
    ));
    public static final List<AgoraRoom> exampleRooms = new ArrayList<>();
    public static final List<MusicModel> exampleSongs = new ArrayList<>();
    public static final List<Integer> exampleBackgrounds = new ArrayList<>(
            Arrays.asList(
                    R.mipmap.ktv_music_background1,
                    R.mipmap.ktv_music_background2,
                    R.mipmap.ktv_music_background3,
                    R.mipmap.ktv_music_background4,
                    R.mipmap.ktv_music_background5,
                    R.mipmap.ktv_music_background6,
                    R.mipmap.ktv_music_background7,
                    R.mipmap.ktv_music_background8,
                    R.mipmap.ktv_music_background9));
    public static final List<Integer> exampleCovers = new ArrayList<>(
            Arrays.asList(
                    R.mipmap.icon_room_cover1,
                    R.mipmap.icon_room_cover2,
                    R.mipmap.icon_room_cover3,
                    R.mipmap.icon_room_cover4,
                    R.mipmap.icon_room_cover5,
                    R.mipmap.icon_room_cover6,
                    R.mipmap.icon_room_cover7,
                    R.mipmap.icon_room_cover8,
                    R.mipmap.icon_room_cover9));
    public static final List<Integer> exampleAvatars = new ArrayList<>(
            Arrays.asList(
                    R.mipmap.portrait01,
                    R.mipmap.portrait02,
                    R.mipmap.portrait03,
                    R.mipmap.portrait04,
                    R.mipmap.portrait05,
                    R.mipmap.portrait06,
                    R.mipmap.portrait07,
                    R.mipmap.portrait08,
                    R.mipmap.portrait09,
                    R.mipmap.portrait10,
                    R.mipmap.portrait11,
                    R.mipmap.portrait12,
                    R.mipmap.portrait13,
                    R.mipmap.portrait14));

    /*
     * init exampleRooms
     */
    static {
        for (int i = 0; i < 6; i++) {
            AgoraRoom room = new AgoraRoom();
            String temp = String.valueOf(i+1);
            room.setId(exampleRoomNames.get(i));
            room.setMv(temp);
            room.setCover(temp);
            room.setChannelName("00"+temp);
            exampleRooms.add(room);
        }
    }

    /*
     * init exampleSongs
     */
    static {
        String jsonData = "[{\"musicId\": \"001\", \"name\": \"七里香\", \"song\": \"https://webdemo.agora.io/ktv/001.mp3\", \"lrc\": \"https://webdemo.agora.io/ktv/001.xml\", \"singer\": \"周杰伦\"},"
                + "{\"musicId\": \"002\", \"name\": \"后来\", \"song\": \"https://webdemo.agora.io/ktv/002.mp3\", \"lrc\": \"https://webdemo.agora.io/ktv/002.xml\", \"singer\": \"刘若英\"}," +
                "{\"musicId\": \"003\", \"name\": \"我怀念的\", \"song\": \"https://webdemo.agora.io/ktv/003.mp3\", \"lrc\": \"https://webdemo.agora.io/ktv/003.xml\", \"singer\": \"孙燕姿\"}, " +
                "{\"musicId\": \"004\", \"name\": \"突然好想你\", \"song\": \"https://webdemo.agora.io/ktv/004.mp3\", \"lrc\": \"https://webdemo.agora.io/ktv/004.xml\", \"singer\": \"五月天\"}]";
        List<MusicModel> data = new Gson().fromJson(jsonData, new TypeToken<List<MusicModel>>() {
        }.getType());
        exampleSongs.addAll(data);
    }

    /**
     * 歌房背景图
     */
    private static final MutableLiveData<Integer> backgroundImage = new MutableLiveData<>(0);

    public static LiveData<Integer> getBackgroundImage() {
        return backgroundImage;
    }

    public static void updateBackgroundImage(int index) {
        backgroundImage.setValue(index);
    }
}
