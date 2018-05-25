//
// Created by 湛孝超 on 2018/5/4.
//

#include "AgoraFFResample.h"
extern  "C"{
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
}

#include "AgoraLog.h"
void AgoraFFResample::Close() {
    mux.lock();
    if (actx)
    {
        swr_free(&actx);
    }
    mux.unlock();
}
bool AgoraFFResample::Open(AgoraParameter in, AgoraParameter out) {
    Close();
    mux.lock();
    //音频重采样上下文
    actx = swr_alloc();
    actx = swr_alloc_set_opts(actx,av_get_default_channel_layout(out.channels),AV_SAMPLE_FMT_S16,out.sample_rate,av_get_default_channel_layout(in.para->channels),(AVSampleFormat)in.para->format,in.para->sample_rate,0,0);
    int re = swr_init(actx);
    if (re != 0){
        mux.unlock();
        XLOGE("swr_init failed!");
        return false;
    } else{
        XLOGI("swr_init success!");
    }
    outChannels = in.para->channels;
    outFormat = AV_SAMPLE_FMT_S16;
    mux.unlock();
    return true;
}

XData AgoraFFResample::Resample(XData indata) {
    if(indata.size <= 0 || !indata.data) return XData();
    mux.lock();
    if(!actx){
        mux.unlock();
        return XData();
    }
    AVFrame *frame = (AVFrame *)indata.data;
    //输出空间的分配
    XData out;
    int outSize = outChannels * frame->nb_samples*av_get_bytes_per_sample((AVSampleFormat)outFormat);
    if(outSize <= 0) return XData();
    out.Alloc(outSize);
    uint8_t  *out_Arr[2] = {0};
    out_Arr[0] = out.data;
    int len = swr_convert(actx,out_Arr,frame->nb_samples,(const uint8_t **)frame->data,frame->nb_samples);
    if(len <= 0){
        mux.unlock();
        out.Drop();
        return XData();
    }
//    uint8_t *tmpBuf = (uint8_t *)malloc((uint8_t)out.size);
////    memcpy(tmpBuf,out.data,(uint8_t)out.size);
////    for (int i = 0; i < out.size; ++i) {
////        tmpBuf[(uint8_t)2i] =  0;
////        tmpBuf[(uint8_t)(2i+1)] = 0;
////    }
//    memcpy(out.data,tmpBuf,(uint8_t)out.size);
//    delete(tmpBuf);
    out.pts = indata.pts;
    mux.unlock();
    return out;

}



