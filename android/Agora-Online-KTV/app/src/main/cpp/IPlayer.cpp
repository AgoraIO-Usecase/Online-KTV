//
// Created by zhanxiaochao on 2018/5/5.
//

#include "IPlayer.h"
#include "AgoraLog.h"
#include "GLVideoView.h"

IPlayer *IPlayer::Get(unsigned char index) {
    static IPlayer p[256];
    return &p[index];
}
void IPlayer::Main() {
    while(!isExit)
    {
        mux.lock();
        if(!audioPlay || !vDecode)
        {
            mux.unlock();
            XSleep(2);
            continue;
        }
        //同步
        //获取音频的pts 告诉视频
        int apts = audioPlay->pts;
        vDecode->synPts = apts;

        mux.unlock();
        XSleep(2);
    }

}


void IAudioPlay::callBackAudioData(XData data) {

    IPlayer::Get()->callBackData(data);

}

void IPlayer::Close() {

    mux.lock();
    //关闭主体线程
    XThread::Stop();
    //解封装
    if(demux) demux->Stop();
    //解码
    if (vDecode) vDecode->Stop();
    if (aDecode) aDecode->Stop();
    if(audioPlay) audioPlay->Stop();

    //清理缓冲队列
    if(vDecode) vDecode->Clear();
    if(aDecode) aDecode->Clear();
    if(audioPlay) audioPlay->Clear();

    //清理资源
    if(audioPlay) audioPlay->Close();
    if(aDecode) aDecode->Close();
    if(vDecode) vDecode->Close();
    if(demux)  demux->Close();
    if(videoView) videoView->Close();

    mux.unlock();
}
double IPlayer::PlayPos() {
    double pos = 0;
    mux.lock();
    int total = 0;
    if(demux)
        total = demux->totalMS;
    if(total>0){
        if(vDecode){
            pos = vDecode->pts/(double)total;
        }

    }
    mux.unlock();
    return pos;
}
void IPlayer::SetPause(bool isP) {
    mux.lock();
    XThread::SetPause(isP);
    if(demux)demux->SetPause(isP);
    if(aDecode)aDecode->SetPause(isP);
    if(vDecode)vDecode->SetPause(isP);
    if(audioPlay)audioPlay->SetPause(isP);
    mux.unlock();
}
bool IPlayer::Seek(double pos) {
    bool re = false;
    if(demux) return false;
    //暂停所有线程
    SetPause(true);
    mux.lock();
    //2 清理缓冲
    if(vDecode) vDecode->Clear();
    if(aDecode) aDecode->Clear();
    if(audioPlay) audioPlay->Clear();

    re = demux->Seek(pos);
    if(!vDecode)
    {
        mux.unlock();
        SetPause(false);
        return re;
    }
    //解码到实际需要显示的帧
    int seekPOS = pos*demux->totalMS;
    while(!isExit)
    {
        XData pkt = demux->Read();
        if(pkt.size<= 0) break;
        if(pkt.isAudio)
        {
            if(pkt.pts<seekPOS)
            {
                pkt.Drop();
                continue;
            }
            //写入缓冲队列
            demux->Notify(pkt);
            continue;
        }
        //解码需要显示的帧之前的数据
        vDecode->SendPacket(pkt);
        pkt.Drop();
        XData data = vDecode->RecvFrame();

        if(data.size <= 0)
        {
            continue;
        }
        if(data.pts >= seekPOS)
        {
            break;
        }
    }
    mux.unlock();
    SetPause(false);
    return re;
}
bool IPlayer::Open(const char *path){
    Close();
    mux.lock();
    //解封装
    if(!demux ||!demux->Open(path)){
        mux.unlock();
        XLOGE("demux -> open failed! ");
        return false;
    }
    //解码 解码不需要 如果是解封之后的就是原始数据
    if(!vDecode ||!vDecode->Open(demux->GetVPara(),isHardDecode)){

        XLOGE("vDecode -> open failed! ");
    }
    //解码音频数据
    if(!aDecode || !aDecode->Open(demux->GetAPara()))
    {
        XLOGE("adecode -> Open %s failed!",path);
    }

    outPara = demux->GetAPara();
    if(!resample || !resample->Open(demux->GetAPara(),outPara))
    {
        XLOGE("resample -> open failed! %s ",path);

    }
    mux.unlock();

    return true;
}
bool IPlayer::Start() {
    mux.lock();
    if(vDecode) vDecode->Start();
    if(!demux || !demux->Start()){
        mux.unlock();
        XLOGE("demux start failed!");
        return false;
    }
    if(aDecode) aDecode->Start();
    if(audioPlay) audioPlay->StartPlay(outPara);
    XThread::Start();
    mux.unlock();
    return true;
}
void IPlayer::InitView(void *win) {
    if(videoView)
    {
        videoView->Close();
        videoView->SetRender(win);
    }
}
void GLVideoView::videoDataCallBack(XData data) {
    IPlayer::Get()->videoCallData(data);
}
void IPlayer::ChangeAudio(bool isChangeAudioStream) {
    demux->ChangeAudioStream(isChangeAudioStream);
}