//
// Created by zhanxiaochao on 2018/5/5.
//

#include "AgoraIPayerProxy.h"
#include "IPlayerBuilder.h"
#include "AgoraFFPlayerBuilder.h"

void AgoraIPayerProxy::Close() {
    mux.lock();
    if(player) player->Close();
    mux.unlock();
}
void AgoraIPayerProxy::Init(void *vm) {
    mux.lock();
    if(vm)
    {
        AgoraFFPlayerBuilder::InitHard(vm);
    }
    if(!player){
        player = AgoraFFPlayerBuilder::Get()->BuilderPlayer();
    }
    mux.unlock();
}
bool AgoraIPayerProxy::IsPause() {
    bool  re = false;
    mux.lock();
    if(player){
        re = player->IsPause();
    }
    mux.unlock();
    return  re;
}

//获取当前的播放进度
double AgoraIPayerProxy::PlayPos() {
    double pos = 0;
    mux.lock();
    if(player)
    {
        pos = player->PlayPos();
    }
    mux.unlock();
    return pos;
}
void AgoraIPayerProxy::SetPause(bool isP) {

    mux.lock();
    if(player) {
        player->SetPause(isP);
    }
    mux.unlock();
}
bool AgoraIPayerProxy::Seek(double pos) {
    bool re = false;
    mux.lock();
    if(player){
        re = player->Seek(pos);
    }
    mux.unlock();
    return re;
}
bool  AgoraIPayerProxy::Open(const char *path) {
    Close();
    bool  re = false;
    mux.lock();
    if(player){
        player->isHardDecode = isHardDecode;
        re = player->Open(path);
    }
    mux.unlock();
    return re;
}
bool AgoraIPayerProxy::Start() {
    bool re = false;
    mux.lock();
    if(player){
        re = player->Start();
    }
    mux.unlock();
    return re;
}
void AgoraIPayerProxy::InitView(void *win) {
    mux.lock();
    if(player){
        player->InitView(win);
    }
    mux.unlock();
}