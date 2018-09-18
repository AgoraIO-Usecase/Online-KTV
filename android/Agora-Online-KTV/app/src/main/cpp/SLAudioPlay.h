//
// Created by zhanxiaochao on 2018/5/5.
//

#ifndef AGORAPLAYERDEMO_SLAUDIOPLAY_H
#define AGORAPLAYERDEMO_SLAUDIOPLAY_H


#include "IAudioPlay.h"

class SLAudioPlay : public IAudioPlay{
public:
    virtual bool StartPlay(AgoraParameter out);
    virtual void Close();
    virtual void SetPlayVolume(double value);
    void PlayCall(void *bufq);
    SLAudioPlay();
    virtual  ~SLAudioPlay();

protected:
    unsigned char *buf = 0;
    std::mutex mux;
};


#endif //AGORAPLAYERDEMO_SLAUDIOPLAY_H
