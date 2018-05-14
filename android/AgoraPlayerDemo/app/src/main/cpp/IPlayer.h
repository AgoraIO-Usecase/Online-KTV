//
// Created by zhanxiaochao on 2018/5/5.
//

#ifndef AGORAPLAYERDEMO_IPLAYER_H
#define AGORAPLAYERDEMO_IPLAYER_H


#include <math.h>
#include "XThread.h"
#include "AgoraParameter.h"
#include "IDemux.h"
#include "IDecode.h"
#include "IResample.h"
#include "IVideoView.h"
#include "IAudioPlay.h"
typedef void (*CallBack)(XData data);
class IPlayer : public  XThread{
public:
    static IPlayer *Get(unsigned char index = 0);
    virtual bool Open(const char *path);
    virtual void Close();
    virtual bool Start();
    virtual void InitView(void *win);

    //获取当前的播放的进度 0.0 ~ 1.0
    virtual double PlayPos();
    virtual bool Seek(double pos);
    virtual void SetPause(bool isP);
    //是否视频硬解码
    bool isHardDecode= true;
    //音频输出参数
    AgoraParameter outPara;

    IDemux *demux = 0;
    IDecode *aDecode = 0;
    IDecode *vDecode = 0;
    IResample *resample = 0;
    IVideoView *videoView = 0;
    IAudioPlay *audioPlay = 0;
    void callBackData(XData  data);
    void videoCallData(XData data);
protected:
    //音视频同步
    void Main();
    std::mutex mux;
    IPlayer(){};
public:
    CallBack audioCallback;
};


#endif //AGORAPLAYERDEMO_IPLAYER_H
