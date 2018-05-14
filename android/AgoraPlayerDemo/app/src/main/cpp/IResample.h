//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_IRESAMPLE_H
#define AGORAPLAYERDEMO_IRESAMPLE_H


#include "IObserver.h"
#include "AgoraParameter.h"

class IResample : public IObserver {
public:
    virtual bool Open(AgoraParameter in,AgoraParameter out = AgoraParameter()) = 0;
    virtual XData Resample(XData indata) = 0;
    virtual void Close() = 0;
    virtual void Update(XData data);
    int outChannels = 2;
    int outFormat = 1;
};


#endif //AGORAPLAYERDEMO_IRESAMPLE_H
