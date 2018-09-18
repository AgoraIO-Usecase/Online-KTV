//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_AGORAFFDEMUX_H
#define AGORAPLAYERDEMO_AGORAFFDEMUX_H

#include "IDemux.h"
struct AVFormatContext;

#include <list>

class AgoraFFDemux : public IDemux{
public:
    int totalSecs;
    //打开文件，或者流媒体 rmtp http rtsp
    virtual bool Open(const char *url);
    //seek 位置 pos 0.0~1.0
    virtual bool Seek(double pos);
    virtual void Close();

    //获取视频参数
    virtual AgoraParameter GetVPara();

    //获取音频参数
    virtual AgoraParameter GetAPara();

    //读取一帧数据，数据由调用者清理
    virtual XData Read();

    AgoraFFDemux();

    virtual void ChangeAudioStream(bool isChangeAudioStream);
    void totalMsCallBack(int time);

private:
    AVFormatContext *ic = 0;
    std::mutex mux;
    int audioStream = 1;
    int videoStream = 0;
    //保存两个音频流的索引
    int audioStream_1 = 0;
    int audioStream_2 = 0;

};


#endif //AGORAPLAYERDEMO_AGORAFFDEMUX_H
