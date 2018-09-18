//
// Created by zhanxiaochao on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_IVIDEOVIEW_H
#define AGORAPLAYERDEMO_IVIDEOVIEW_H


#include "IObserver.h"

class IVideoView: public IObserver {
public:
    virtual void SetRender(void *win) = 0;
    virtual void Render(XData data) = 0;
    virtual void Update(XData data);
    virtual void Close() = 0;
};


#endif //AGORAPLAYERDEMO_IVIDEOVIEW_H
