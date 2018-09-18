//
// Created by 湛孝超 on 2018/5/4.
//

#include "AgoraFFDemux.h"
#include "AgoraLog.h"

extern "C"{
#include <libavformat/avformat.h>
}

//分数转为浮点数
static double r2d(AVRational r)
{
    return r.num == 0 || r.den == 0 ?0.:(double) r.num/(double)r.den;
}
void AgoraFFDemux::Close()
{
    mux.lock();
    if(ic)
        avformat_close_input(&ic);
    mux.unlock();
}

//seek 位置 pos 0.0~1.0
bool AgoraFFDemux::Seek(double pos)
{
    if(pos<0 || pos > 1)
    {
        XLOGE("Seek value must 0.0~1.0");
        return false;
    }
    bool re = false;
    mux.lock();
    if(!ic)
    {
        mux.unlock();
        return false;
    }
    //清理读取的缓冲
    avformat_flush(ic);
    long long seekPts = 0;
    seekPts = ic->streams[videoStream]->duration*pos;
    //往后跳转到关键帧
    re = av_seek_frame(ic,videoStream,seekPts,AVSEEK_FLAG_FRAME|AVSEEK_FLAG_BACKWARD);
    mux.unlock();
    return re;
}

//打开文件，或者流媒体 rmtp http rtsp
bool AgoraFFDemux::Open(const char *url)
{
    XLOGI("Open file %s begin",url);
    Close();
    mux.lock();
    int re = avformat_open_input(&ic,url,0,0);
    if(re != 0 )
    {
        mux.unlock();
        char buf[1024] = {0};
        av_strerror(re,buf,sizeof(buf));
        XLOGE("FFDemux open %s failed! %s",url,buf);
        return false;
    }
    XLOGI("FFDemux open %s success!",url);

    //读取文件信息
    re = avformat_find_stream_info(ic,0);
    if(re != 0 )
    {
        mux.unlock();
        char buf[1024] = {0};
        av_strerror(re,buf,sizeof(buf));
        XLOGE("avformat_find_stream_info %s failed!",url);
        return false;
    }

    this->totalMS = ic->duration/(AV_TIME_BASE/1000);

    mux.unlock();
    XLOGI("total ms = %d!",totalMS);

    this->totalMsCallBack(totalMS);
    GetVPara();
    GetAPara();
    return true;
}
//获取视频参数
AgoraParameter AgoraFFDemux::GetVPara()
{
    mux.lock();
    if (!ic) {
        mux.unlock();
        XLOGE("GetVPara failed! ic is NULL！");
        return AgoraParameter();
    }
    //获取了视频流索引
    int re = av_find_best_stream(ic, AVMEDIA_TYPE_VIDEO, -1, -1, 0, 0);
    if (re < 0) {
        mux.unlock();
        XLOGE("av_find_best_stream failed!");
        return AgoraParameter();
    }
    videoStream = re;
    AgoraParameter para;
    para.para = ic->streams[re]->codecpar;
    mux.unlock();
    return para;
}
//获取音频参数
AgoraParameter AgoraFFDemux::GetAPara()
{
    mux.lock();
    if (!ic) {
        mux.unlock();
        XLOGE("GetVPara failed! ic is NULL！");
        return AgoraParameter();
    }
    //获取多音轨视频的音频流索引
    for (int i = 0; i < ic->nb_streams; ++i) {
        if(ic->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO)
        {
            if (audioStream_1 == 0){
                audioStream_1 = i;
                continue;
            }
            if(audioStream_1 != 0 && audioStream_2 == 0)
            {
                audioStream_2 = i;
                continue;
            }
        }
    }
//    int re = av_find_best_stream(ic, AVMEDIA_TYPE_AUDIO, -1, -1, 0, 0);
//    if (re < 0) {
//        mux.unlock();
//        XLOGE("av_find_best_stream failed!");
//        return AgoraParameter();
//    }
//    audioStream = re;
    AgoraParameter para;
    para.para = ic->streams[audioStream]->codecpar;
    para.channels = ic->streams[audioStream]->codecpar->channels;
    para.sample_rate = ic->streams[audioStream]->codecpar->sample_rate;
    mux.unlock();
    return para;
}
//读取一帧数据，数据由调用者清理
XData AgoraFFDemux::Read()
{
    mux.lock();
    if(!ic)
    {
        mux.unlock();
        return XData();
    }

    XData d;
    AVPacket *pkt = av_packet_alloc();
    int re = av_read_frame(ic,pkt);
    if(re != 0)
    {
        mux.unlock();
        av_packet_free(&pkt);
        return XData();
    }
    //XLOGI("pack size is %d ptss %lld",pkt->size,pkt->pts);
    d.data = (unsigned char*)pkt;
    d.size = pkt->size;

    if(pkt->stream_index == audioStream)
    {

        d.isAudio = true;
    }
    else if(pkt->stream_index == videoStream)
    {
        d.isAudio = false;
    }
    else
    {
        mux.unlock();
        av_packet_free(&pkt);
        return XData();
    }

    //转换pts
    pkt->pts = pkt->pts * (1000*r2d(ic->streams[pkt->stream_index]->time_base));
    pkt->dts = pkt->dts * (1000*r2d(ic->streams[pkt->stream_index]->time_base));
    d.pts = (int)pkt->pts;
    //XLOGE("demux pts %d",d.pts);


    mux.unlock();
    return d;
}



AgoraFFDemux::AgoraFFDemux()
{
    static bool isFirst = true;
    if(isFirst)
    {
        isFirst = false;
        //注册所有封装器
        av_register_all();

        //注册所有的解码器
        avcodec_register_all();

        //初始化网络
        avformat_network_init();
        XLOGI("register ffmpeg!");
    }
}
void AgoraFFDemux::ChangeAudioStream(bool isChangeAudioStream) {
    if (isChangeAudioStream)
    {
        if(audioStream_2 == 0)
            return;
        audioStream = audioStream_2;

    } else
    {   if(audioStream_1 == 0)
            return;
        audioStream = audioStream_1;
    }

}