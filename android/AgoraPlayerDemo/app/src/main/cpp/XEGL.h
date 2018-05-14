//
// Created by zhanxiaochao on 2018/5/5.
//

#ifndef AGORAPLAYERDEMO_XEGL_H
#define AGORAPLAYERDEMO_XEGL_H


class XEGL {
public:
    virtual bool Init(void *win) = 0;
    virtual void Close() = 0;
    virtual void Draw() = 0;
    static XEGL *Get();

protected:
    XEGL(){};
};


#endif //AGORAPLAYERDEMO_XEGL_H
