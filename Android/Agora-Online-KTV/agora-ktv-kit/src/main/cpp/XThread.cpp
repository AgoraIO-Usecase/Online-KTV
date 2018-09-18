//
// Created by 湛孝超 on 2018/5/4.
//

#include "XThread.h"
#include <thread>
#include "AgoraLog.h"
using namespace std;
void XSleep(int mis){
    chrono::milliseconds du(mis);
    this_thread::sleep_for(du);
}
void XThread::SetPause(bool isP) {
    isPause = isP;
    //等待100毫秒
    for (int i = 0; i < 10; ++i) {
        if (isPausing == isP)
        {
            break;
        }
        XSleep(10);
    }
}
//启动线程
bool XThread::Start() {
    isExit = false;
    isPause = false;
    thread th(&XThread::ThreadMain,this);
    th.detach();
    return true;
}
void XThread::ThreadMain() {
    isRunning = true;
    XLOGI("线程函数进入");
    Main();
    XLOGI("线程函数的退出");
    isRunning = false;

}
//通过控制isExit 安全停止线程
void XThread::Stop() {
    XLOGI("stop 停止线程 begin");
    isExit = true;
    isPause = false;
    for (int i = 0; i < 200; ++i) {
        if (!isRunning)
        {
            XLOGI("停止线程成功");
            return;
        }
        XSleep(1);
    }
    XLOGI("Stop 停止线程超时！");
}