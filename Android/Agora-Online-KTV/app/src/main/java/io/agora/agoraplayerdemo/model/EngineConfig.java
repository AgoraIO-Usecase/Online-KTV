package io.agora.agoraplayerdemo.model;

/**
 * Created by zhanxiaochao on 2018/9/3.
 */

public class EngineConfig {
    public  int mClientRole;
    public   int mVideoProfile;
    public int mUid;
    public String mChannel;
    public void reset(){
        mChannel = null;
    }
    EngineConfig(){
    }
}
