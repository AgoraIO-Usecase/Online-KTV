//
// Created by zhanxiaochao on 2018/5/5.
//

#ifndef AGORAPLAYERDEMO_AGORAFFPLAYERBUILDER_H
#define AGORAPLAYERDEMO_AGORAFFPLAYERBUILDER_H


#include "IPlayerBuilder.h"

class AgoraFFPlayerBuilder : public IPlayerBuilder {
public:
    static void InitHard(void *vm);
    static AgoraFFPlayerBuilder *Get(){
        static AgoraFFPlayerBuilder ff;
        return &ff;
    }

protected:
    AgoraFFPlayerBuilder(){};
    virtual IDemux *CreateDemux();
    virtual IDecode *CreateDecode();
    virtual IResample *CreateResample();
    virtual IVideoView *CreateVideoView();
    virtual IAudioPlay *CreateAudioPlay();
    virtual IPlayer *CreatePlayer(unsigned char index = 0);

};


#endif //AGORAPLAYERDEMO_AGORAFFPLAYERBUILDER_H
