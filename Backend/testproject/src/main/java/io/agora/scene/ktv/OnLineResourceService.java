package io.agora.scene.ktv;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;

public class OnLineResourceService {
    private static final String SERVER_PREFIX = "https://api.agora.io/cn/v1.0/projects/";

    private static final Gson gson = new Gson();

    @Value("appid")
    private static final String appid = "";

    @Value("token")
    private static final String token = "";

    private String getUrl(){
        return String.format(SERVER_PREFIX+ "%s/ktv-service/", appid);
    }

    @Cacheable(value = "MUSIC",key = "#id")
    public String getMusicUrl(String id) throws IOException {
        String url = getUrl() + "api/serv/song-url?requestId=1&lyricType=1&songCode=" + id;
        String res = this.getMethod(url);
        UrlResponse response = gson.fromJson(res, UrlResponse.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("song", response.data.playUrl);
        jsonObject.addProperty("lrc", response.data.lyric);
        return jsonObject.toString();
    }

    private String getMethod(String url) throws IOException {
        Request request = new Request.Builder()
                .header("Authorization", "Basic " + token)
                .url(url)
                .get()
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        final Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        return response.body().string();
    }

    public static void main(String[] args) throws IOException {
        OnLineResourceService onLineResourceService = new OnLineResourceService();
        System.out.println(new OnLineResourceService().getMusicUrl("6246262727339400"));
    }

    class Data{
        private String playUrl;
        private String lyric;
    }

    class UrlResponse{
        private Data data;
    }

}
