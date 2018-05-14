//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_IOBSERVER_H
#define AGORAPLAYERDEMO_IOBSERVER_H

#include <vector>
#include <mutex>
#include "XThread.h"
#include "XData.h"

class IObserver:public XThread{
public:
    //观察者接收数据函数
    virtual void Update(XData data){}
    //主体函数 添加观察者
    virtual void AddObs(IObserver *obs);
    //通知所有观察者（线程安全）
    void Notify(XData data);


protected:
    std::vector<IObserver *>obss;
    std::mutex mux;
};


#endif //AGORAPLAYERDEMO_IOBSERVER_H
