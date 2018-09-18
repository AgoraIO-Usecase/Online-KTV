//
// Created by zhanxiaochao on 2018/5/5.
//

#include "AgoraFFPlayerBuilder.h"
#include "AgoraFFDemux.h"
#include "AgoraFFDecode.h"
#include "AgoraFFResample.h"
#include "GLVideoView.h"
#include "SLAudioPlay.h"

IDemux *AgoraFFPlayerBuilder::CreateDemux() {
    IDemux *de = new AgoraFFDemux();
    return de;
}
IDecode *AgoraFFPlayerBuilder::CreateDecode() {
    IDecode *de = new AgoraFFDecode();
    return de;
}
IResample *AgoraFFPlayerBuilder::CreateResample() {
    IResample *ff = new AgoraFFResample();
    return ff;
}
IVideoView *AgoraFFPlayerBuilder::CreateVideoView() {
    IVideoView *videoView = new GLVideoView();
    return videoView;
}
IAudioPlay *AgoraFFPlayerBuilder::CreateAudioPlay() {
    IAudioPlay *audioPlay = new SLAudioPlay();
    return audioPlay;
}
IPlayer *AgoraFFPlayerBuilder::CreatePlayer(unsigned char index) {
    return IPlayer::Get(index);
}
void AgoraFFPlayerBuilder::InitHard(void *vm) {
    AgoraFFDecode::InitHard(vm);
}