//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_AGORAFFDECODE_H
#define AGORAPLAYERDEMO_AGORAFFDECODE_H

#include "IDecode.h"

struct AVCodecContext;
struct AVFrame;
class AgoraFFDecode : public  IDecode {
public:
    static void InitHard(void *vm);

    virtual bool Open(AgoraParameter para,bool isHard=false);
    virtual void Close();
    virtual void Clear();
    //future模型 发送数据到线程解码
    virtual bool SendPacket(XData pkt);
    //从线程中获取解码结果，再次调用会复用上次空间，线程不安全
    virtual XData RecvFrame();

protected:
    AVCodecContext *codec = 0;
    AVFrame *frame = 0;
    std::mutex mux;
};


#endif //AGORAPLAYERDEMO_AGORAFFDECODE_H
