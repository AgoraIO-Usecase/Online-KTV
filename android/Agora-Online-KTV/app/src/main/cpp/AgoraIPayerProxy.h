//
// Created by zhanxiaochao on 2018/5/5.
//

#ifndef AGORAPLAYERDEMO_AGORAIPAYERPROXY_H
#define AGORAPLAYERDEMO_AGORAIPAYERPROXY_H


#include "IPlayer.h"

class AgoraIPayerProxy : public IPlayer {
public:
    static AgoraIPayerProxy *Get(){
        static AgoraIPayerProxy px;
        return &px;
    }
    void Init(void *vm = 0);
    virtual bool Seek(double pos);
    virtual bool Open(const char *path);
    virtual void Close();
    virtual bool Start();
    virtual void InitView(void *win);
    virtual void SetPause(bool isP);
    virtual bool IsPause();
    //获取当前的播放进度
    virtual double PlayPos();
    virtual void ChangeAudio(bool isChangeAudioStream);

public:
    AgoraIPayerProxy(){};
    IPlayer *player = 0;
    std::mutex mux;
    bool isChangeAudioStream = false;
};


#endif //AGORAPLAYERDEMO_AGORAIPAYERPROXY_H
