package io.agora.agoraplayerdemo.Model;

import android.view.SurfaceView;

/**
 * Created by zhanxiaochao on 2018/9/3.
 */

public class VideoStatusData {
    public static final int DEAULT_STATUS = 0;
    public static final int VIDEO_MUTED = 1;
    public static final int AUDIO_MUTED = VIDEO_MUTED << 1;
    public static final int DEFAULT_VOLUME = 0;
    public VideoStatusData(int uid, SurfaceView view , int status ,int volume){

    }
    public int mUid;
    public  SurfaceView mView;
    public int mStatus;
    public int mVolume;
    @Override
    public String toString(){
        return "VideoStatusData{" +
                "mUid=" + (mUid & 0xFFFFFFFFL) +
                ", mView=" + mView +
                ", mStatus=" + mStatus +
                ", mVolume=" + mVolume +
                '}';
    }
}
