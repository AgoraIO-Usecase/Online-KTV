//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_XTHREAD_H
#define AGORAPLAYERDEMO_XTHREAD_H
//sleep
void XSleep(int mis);

class XThread {
public:
    //启动线程
    virtual bool Start();
    //通过控制isExit安全停止线程 (不一定成功)
    virtual void Stop();
    virtual void SetPause(bool isP);
    virtual bool IsPause()
    {
        isPausing = isPause;
        return isPause;
    }
    //入口主函数
    virtual void Main(){}
protected:
    bool isExit = false;
    bool isRunning = false;
    bool isPause = false;
    bool isPausing = false;
private:
    void ThreadMain();
};


#endif //AGORAPLAYERDEMO_XTHREAD_H
