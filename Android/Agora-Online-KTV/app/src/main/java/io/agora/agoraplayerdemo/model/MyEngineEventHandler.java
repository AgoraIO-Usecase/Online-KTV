package io.agora.agoraplayerdemo.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.rtc.IRtcEngineEventHandler;

/**
 * Created by zhanxiaochao on 2018/9/3.
 */

public class MyEngineEventHandler {
    public MyEngineEventHandler(Context ctx,EngineConfig config){
        this.mConfig = config;
        this.mContext = ctx;
    }
    private final EngineConfig mConfig;
    private final Context mContext;
    private final ConcurrentHashMap<AGEventHandler,Integer> mEventHandleList = new ConcurrentHashMap<>();
    public void addEventHandler(AGEventHandler handler){this.mEventHandleList.put(handler,0);}

    public void removeEventHandler(AGEventHandler handler){
        this.mEventHandleList.remove(handler);
    }

    final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        private final Logger log =  LoggerFactory.getLogger(this.getClass());
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            Iterator<AGEventHandler> it = mEventHandleList.keySet().iterator();
            while (it.hasNext())
            {
                AGEventHandler handler = it.next();
                handler.onJoinChannelSuccess(channel,uid,elapsed);
            }

        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            log.debug("onUserJoined " + (uid & 0xFFFFFFFFL) + " " + elapsed);
            Iterator <AGEventHandler> it = mEventHandleList.keySet().iterator();
            while (it.hasNext()){
                AGEventHandler handler = it.next();
                handler.onUserJoined(uid,elapsed);
            }
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            log.debug("onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
            Iterator <AGEventHandler> it = mEventHandleList.keySet().iterator();
            while (it.hasNext()){
                AGEventHandler handler = it.next();
                handler.onUserOffline(uid,reason);
            }

        }

        @Override
        public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed) {
            super.onFirstRemoteVideoFrame(uid, width, height, elapsed);
        }

        @Override
        public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
            super.onFirstLocalVideoFrame(width, height, elapsed);
        }

        @Override
        public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
            Iterator<AGEventHandler> it = mEventHandleList.keySet().iterator();
            while (it.hasNext()){
                AGEventHandler handler = it.next();
                handler.onFirstRemoteVideoDecoded(uid,width,height,elapsed);
            }
        }
    };



}
