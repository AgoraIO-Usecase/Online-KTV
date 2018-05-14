//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_AGORAFFRESAMPLE_H
#define AGORAPLAYERDEMO_AGORAFFRESAMPLE_H

#include "AgoraParameter.h"
#include "XData.h"
#include "IResample.h"
#include <mutex>

struct SwrContext;
class AgoraFFResample : public IResample {
public:
    virtual bool Open(AgoraParameter in,AgoraParameter out = AgoraParameter());
    virtual void Close();
    virtual XData Resample(XData indata);

protected:
    SwrContext *actx = 0;
    std::mutex mux;
};


#endif //AGORAPLAYERDEMO_AGORAFFRESAMPLE_H
