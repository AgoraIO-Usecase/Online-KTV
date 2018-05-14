//
// Created by zhanxiaochao on 2018/5/5.
//

#include "IPlayerBuilder.h"
IPlayer *IPlayerBuilder::BuilderPlayer(unsigned char index) {
    IPlayer *play = CreatePlayer(index);
    IDemux *demux = CreateDemux();
    IDecode *vDecode = CreateDecode();
    IDecode *aDecode = CreateDecode();
    demux->AddObs(aDecode);
    demux->AddObs(vDecode);

    //显示视频观察解码器
    IVideoView *videoView = CreateVideoView();
    vDecode->AddObs(videoView);
    //重采样解码器

    IResample *resample = CreateResample();
    aDecode->AddObs(resample);

    //音频重采样播放器
    IAudioPlay *audioPlay = CreateAudioPlay();
    resample->AddObs(audioPlay);

    play->demux = demux;
    play->vDecode = vDecode;
    play->aDecode = aDecode;
    play->resample = resample;
    play->audioPlay = audioPlay;
    play->videoView = videoView;
    return play;
}