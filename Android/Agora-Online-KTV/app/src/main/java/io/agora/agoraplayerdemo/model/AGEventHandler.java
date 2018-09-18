package io.agora.agoraplayerdemo.model;

/**
 * Created by zhanxiaochao on 2018/9/3.
 */

public interface AGEventHandler{
    void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed);

    void onJoinChannelSuccess(String channel, int uid, int elapsed);

    void onUserOffline(int uid, int reason);

    void onUserJoined(int uid, int elapsed);
}
