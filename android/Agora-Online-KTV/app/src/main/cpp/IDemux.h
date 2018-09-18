//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_IDEMUX_H
#define AGORAPLAYERDEMO_IDEMUX_H


#include "IObserver.h"
#include "AgoraParameter.h"

class IDemux : public IObserver {
public:
    //打开文件
    virtual  bool Open(const char *url) = 0;
    //seek
    virtual bool Seek(double pos) = 0;
    virtual void Close() = 0;
    //获取视频参数
    virtual AgoraParameter GetVPara() = 0;
    //获取音频参数
    virtual AgoraParameter GetAPara() = 0;
    //读取一帧数据 数据由调用者处理
    virtual XData Read() = 0;
    //总时长
    int totalMS= 0;
    virtual void ChangeAudioStream(bool isChangeAudioStream) = 0;

protected:
    virtual void Main();
};


#endif //AGORAPLAYERDEMO_IDEMUX_H
