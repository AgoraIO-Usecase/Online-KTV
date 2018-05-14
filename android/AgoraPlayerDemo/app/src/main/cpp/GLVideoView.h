//
// Created by zhanxiaochao on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_GLVIDEOVIEW_H
#define AGORAPLAYERDEMO_GLVIDEOVIEW_H


#include "IVideoView.h"
#include "XTexture.h"

class GLVideoView : public IVideoView {
public:
    virtual void SetRender(void *win);
    virtual void Render(XData data);
    virtual void Close();
    void videoDataCallBack(XData data);

protected:
    void  *view = 0;
    XTexture *txt = 0;
    std::mutex mux;

};


#endif //AGORAPLAYERDEMO_GLVIDEOVIEW_H
