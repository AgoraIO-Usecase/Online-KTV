//
// Created by zhanxiaochao on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_IAUDIOPLAY_H
#define AGORAPLAYERDEMO_IAUDIOPLAY_H


#include "IObserver.h"
#include "AgoraParameter.h"
#include <list>

class IAudioPlay : public IObserver {
public:
    //压入缓冲数据
    virtual void Update(XData data);
    //获取缓冲数据
    virtual XData GetData();
    virtual bool StartPlay(AgoraParameter out) = 0;
    virtual void Close() = 0;
    virtual void Clear();
    virtual void SetPlayVolume(double value) = 0;
    //最大缓冲
    int maxFrame = 100;
    int pts = 0;
    void callBackAudioData(XData data);
    void callBackPts(int pts);
protected:
    std::list <XData > frames;
    std::mutex framesMutex;
};


#endif //AGORAPLAYERDEMO_IAUDIOPLAY_H
