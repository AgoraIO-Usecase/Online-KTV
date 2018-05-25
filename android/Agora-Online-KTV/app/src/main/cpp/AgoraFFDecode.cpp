//
// Created by 湛孝超 on 2018/5/4.
//
extern "C"
{

#include <libavcodec/avcodec.h>
#include <libavcodec/jni.h>

}
#include "AgoraFFDecode.h"
#include "AgoraLog.h"

//初始化硬解码器
void AgoraFFDecode ::InitHard(void *vm) {
    av_jni_set_java_vm(vm,0);
}
void AgoraFFDecode::Clear() {
    IDecode::Clear();
    mux.lock();
    if (codec)
    {
        avcodec_flush_buffers(codec);
    }
    mux.unlock();
}
void AgoraFFDecode::Close() {
    IDecode::Clear();
    mux.lock();
    pts = 0;
    if(frame)
    {
        av_frame_free(&frame);
    }
    if(codec)
    {
        avcodec_close(codec);
        avcodec_free_context(&codec);
    }
    mux.unlock();
}

bool AgoraFFDecode::Open(AgoraParameter para, bool isHard) {
    Close();
    if (!para.para) return false;
    AVCodecParameters *p = para.para;
    //查找解码器
    AVCodec *cd = avcodec_find_decoder(p->codec_id);
    if(isHard)
    {
        cd = avcodec_find_decoder_by_name("h264_mediacodec");
    }
    if(!cd)
    {
        XLOGE("codec find failed!");
        return false;
    }
    XLOGI("codec find success!");
    mux.lock();
    codec = avcodec_alloc_context3(cd);
    avcodec_parameters_to_context(codec,p);
    codec->thread_count = 8;
    //3 打开解码器
    //打开解码器
    int re = avcodec_open2(codec, 0, 0);
    if(re != 0)
    {
        mux.unlock();
        char buf[1024] = {0};
        av_strerror(re,buf, sizeof(buf)-1);
        XLOGE("%s",buf);
        return false;
    }
    if(codec->codec_type == AVMEDIA_TYPE_VIDEO)
    {
        this->isAudio = false;
    } else{
        this->isAudio = true;
    }
    mux.unlock();
    XLOGI("avcodec_open2 success!");
    return true;
}
bool AgoraFFDecode::SendPacket(XData pkt) {
    if(pkt.size <= 0 || !pkt.data) return false;
    mux.lock();
    if(!codec)
    {
        mux.unlock();
        return false;
    }
    int re = avcodec_send_packet(codec,(AVPacket *)pkt.data);
    mux.unlock();
    if(re != 0)
    {
        return false;
    }
//    XLOGI("我正在发包");
    return true;
}
XData AgoraFFDecode::RecvFrame() {
    mux.lock();
    if(!codec){
        mux.unlock();
        return XData();
    }
    if(!frame)
    {
        frame = av_frame_alloc();
    }
    int re = avcodec_receive_frame(codec,frame);
    if (re != 0)
    {
        mux.unlock();
        return  XData();
    }
//    XLOGI("我正在收包");
    XData d;
    d.data = (unsigned char *)frame;
    if(codec->codec_type == AVMEDIA_TYPE_VIDEO)
    {
        d.size = (frame->linesize[0] + frame->linesize[1]+frame->linesize[2]*frame->height);
        XLOGE("%d",frame->linesize[0]);
        //当解码分辨率 大于显示分辨率时 需要将视频的宽替换成解码分辨率
        if(frame->linesize[0] > frame->width){
            d.width = frame->linesize[0];
        } else{
            d.width = frame->width ;
        }
        d.height = frame->height;
    } else{
        //样本字节数 * 单通道样本数 * 通道数
        d.size = av_get_bytes_per_sample((AVSampleFormat)frame->format)*frame->nb_samples * 2;
    }
    d.format = frame->format;
    memcpy(d.datas,frame->data, sizeof(d.datas));
    d.pts = frame->pts;
    pts = d.pts;
    mux.unlock();
    return  d;
}