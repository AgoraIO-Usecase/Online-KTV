//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_IDECODE_H
#define AGORAPLAYERDEMO_IDECODE_H

#include "IObserver.h"
#include "AgoraParameter.h"
#include <list>
class IDecode : public IObserver{

public:
    //打开解码器
    virtual bool Open(AgoraParameter para,bool isHard=false) = 0;
    virtual void Close() = 0;
    virtual void Clear();
    //future模型 发送数据到线程解码
    virtual bool SendPacket(XData pkt) = 0;

    //从线程中获取解码结果  再次调用会复用上次空间，线程不安全
    virtual XData RecvFrame() = 0;

    //由主体notify的数据 阻塞
    virtual void Update(XData pkt);

    bool isAudio = false;

    //最大的队列缓冲
    int maxList = 100;

    //同步时间，再次打开文件要清理
    int synPts = 0;
    int pts = 0;

protected:
    virtual void Main();

    //读取缓冲
    std::list<XData> packs;
    std::mutex packsMutex;

};


#endif //AGORAPLAYERDEMO_IDECODE_H
