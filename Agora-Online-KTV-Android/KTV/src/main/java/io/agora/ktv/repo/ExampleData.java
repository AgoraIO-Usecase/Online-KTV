package io.agora.ktv.repo;

import androidx.annotation.DrawableRes;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.agora.ktv.R;

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
                    R.drawable.ktv_music_background1,
                    R.drawable.ktv_music_background2,
                    R.drawable.ktv_music_background3,
                    R.drawable.ktv_music_background4,
                    R.drawable.ktv_music_background5,
                    R.drawable.ktv_music_background6,
                    R.drawable.ktv_music_background7,
                    R.drawable.ktv_music_background8,
                    R.drawable.ktv_music_background9));
    public static final List<Integer> exampleCovers = new ArrayList<>(
            Arrays.asList(
                    R.drawable.icon_room_cover1,
                    R.drawable.icon_room_cover2,
                    R.drawable.icon_room_cover3,
                    R.drawable.icon_room_cover4,
                    R.drawable.icon_room_cover5,
                    R.drawable.icon_room_cover6,
                    R.drawable.icon_room_cover7,
                    R.drawable.icon_room_cover8,
                    R.drawable.icon_room_cover9));
    public static final List<Integer> exampleAvatars = new ArrayList<>(
            Arrays.asList(
                    R.drawable.portrait01,
                    R.drawable.portrait02,
                    R.drawable.portrait03,
                    R.drawable.portrait04,
                    R.drawable.portrait05,
                    R.drawable.portrait06,
                    R.drawable.portrait07,
                    R.drawable.portrait08,
                    R.drawable.portrait09,
                    R.drawable.portrait10,
                    R.drawable.portrait11,
                    R.drawable.portrait12,
                    R.drawable.portrait13,
                    R.drawable.portrait14));

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

    @DrawableRes
    public static int getCoverRes(String cover){
        int coverIndex = 0;
        try {
            coverIndex = Integer.parseInt(cover);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if(coverIndex >= ExampleData.exampleCovers.size()|| coverIndex < 0) coverIndex = 0;
        return ExampleData.exampleCovers.get(coverIndex);
    }

    @DrawableRes
    public static int getAvatarRes(String avatar){
        int avatarIndex = 0;
        try {
            avatarIndex = Integer.parseInt(avatar);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if(avatarIndex >= ExampleData.exampleAvatars.size()|| avatarIndex < 0) avatarIndex = 0;
        return ExampleData.exampleAvatars.get(avatarIndex);
    }
}