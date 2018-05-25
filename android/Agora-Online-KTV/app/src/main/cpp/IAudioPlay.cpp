//
// Created by zhanxiaochao on 2018/5/4.
//

#include "IAudioPlay.h"
#include "AgoraLog.h"

void IAudioPlay::Clear() {
    framesMutex.lock();
    while (!frames.empty())
    {
        frames.front().Drop();
        frames.pop_front();
    }
    framesMutex.unlock();
}
XData IAudioPlay::GetData() {
    XData d;
    isRunning = true;
    while(!isExit)
    {
        if(IsPause()){
            XSleep(2);
            continue;
        }
        framesMutex.lock();
        if(!frames.empty()){
            //有数据返回
//            XLOGI("获取音频数据");
            d = frames.front();
            frames.pop_front();
            framesMutex.unlock();

            pts = d.pts;
            this->callBackAudioData(d);
            return d;
        }
        framesMutex.unlock();
        XSleep(1);
    }
    XLOGI("未获取数据");
    isRunning = false;

    return  d;

}

void IAudioPlay::Update(XData data) {
    //压入缓冲队列
    if(data.size <= 0 || !data.data){
        return;
    }
    while(!isExit){
        framesMutex.lock();
        if(frames.size()>maxFrame){
            framesMutex.unlock();
            XSleep(1);
            continue;
        }
        data.isAudio = true;
        frames.push_back(data);
        framesMutex.unlock();
        break;
    }
}

