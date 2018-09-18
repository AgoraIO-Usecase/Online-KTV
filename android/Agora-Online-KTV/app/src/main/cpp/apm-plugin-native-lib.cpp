#include <jni.h>
#include <string>
#include "AgoraIPayerProxy.h"
#include "AgoraLog.h"
#include <android/native_window_jni.h>
#include "IAgoraMediaEngine.h"
#include "IAgoraRtcEngine.h"
#include "VMUtil .h"
#include "XShader.h"
#include "AudioCircularBuffer.h"
#include <iostream>

using namespace AgoraRTC;
static scoped_ptr<AudioCircularBuffer<char>> agoraAudioBuf(new AudioCircularBuffer<char>(2048,true));
static scoped_ptr<AudioCircularBuffer<short>> earBackBuf(new AudioCircularBuffer<int16_t>(2048,true));

static bool  isComplete  = false;
static int totalTime= 0;
jobject gCallBack = nullptr;
jclass gCallbackClass = nullptr;
jmethodID setExternalVideoFrameID = nullptr;
jmethodID videoCompleteID = nullptr;
jmethodID totalTimeID = nullptr;
static JavaVM *gJVM = nullptr;
std::mutex mux;
extern "C" {
#include <libavformat/avformat.h>
#include <libyuv/convert.h>
}
extern "C"
JNIEXPORT
jint JNI_OnLoad(JavaVM *vm, void *res) {

    AgoraIPayerProxy::Get()->Init(vm);
    return JNI_VERSION_1_4;
}

class AgoraAudioFrameObserver : public agora::media::IAudioFrameObserver {

public:
    AgoraAudioFrameObserver() {
        gCallBack = nullptr;
        audio_sonSum = 1.00f;
        audio_voiceSum = 1.00f;
    }

    ~AgoraAudioFrameObserver() {
    }


public:
    double  audio_sonSum;
    double  audio_voiceSum;
    void pushAudioData(XData data){
        mux.lock();
//        if(!readPcmPointer) {
//            char *path = "/sdcard/file_in_7.pcm";
//            readPcmPointer = fopen(path, "wb");
//        }
//        fwrite(data.data, 2, data.size, readPcmPointer);

        //采集端的音频
        char *audioBuf =  (char *)malloc(sizeof(char)* data.size);
        memcpy(audioBuf, data.data, data.size);
//        audioPool.add_elements(audioBuf, (int)data.size);
        agoraAudioBuf->Push(audioBuf,data.size);
        delete(audioBuf);
        mux.unlock();

    }
    virtual bool onRecordAudioFrame(AudioFrame &audioFrame) override {
        mux.lock();
        //回调数据
        int16  bytes = audioFrame.samples * audioFrame.channels * audioFrame.bytesPerSample;

        if (agoraAudioBuf->mAvailSamples < bytes) {
            mux.unlock();
            return true;
        }
        char *data = (char *)malloc(sizeof(char) * bytes);
        agoraAudioBuf->Pop(data,(int)bytes);
        int16 *mixedBuffer = (int16 *)data;
        int16 *tmpBuf = (int16 *)malloc((int16)bytes);
        memcpy(tmpBuf, audioFrame.buffer, (int16)bytes);
        for (int i = 0 ; i < (int16)bytes/2; i++) {
            int tmp = (int)(((double)1.00f * mixedBuffer[i]) * audio_sonSum);
            tmpBuf[i]  = (int)(((double)1.00f * tmpBuf[i]) * audio_voiceSum);
            tmp += tmpBuf[i];
            //防溢出的处理
            if (tmp > 32767) {
                tmpBuf[i] = 32767;
            }
            else if (tmp < -32768) {
                tmpBuf[i] = -32768;
            }
            else {
                tmpBuf[i] = tmp;
            }
        }
        memcpy(audioFrame.buffer, tmpBuf,(int16)bytes);
        free(mixedBuffer);
        free(tmpBuf);
        mux.unlock();
        return true;
    }

    virtual bool onPlaybackAudioFrame(AudioFrame &audioFrame) override {
        return true;
    }

    virtual bool
    onPlaybackAudioFrameBeforeMixing(unsigned int uid, AudioFrame &audioFrame) override {
        return true;
    }

    virtual bool onMixedAudioFrame(AudioFrame &audioFrame) override {
        return true;
    }
};


static AgoraAudioFrameObserver s_audioFrameObserver;

static agora::rtc::IRtcEngine *rtcEngine = nullptr;

#ifdef __cplusplus
extern "C" {
#endif

int __attribute__((visibility("default")))
loadAgoraRtcEnginePlugin(agora::rtc::IRtcEngine *engine) {
    XLOGI("loadAgoraRtcEnginePlugin--------- ");
    __android_log_print(ANDROID_LOG_DEBUG, "plugin", "plugin loadAgoraRtcEnginePlugin");
    rtcEngine = engine;
    return 0;
}
void __attribute__((visibility("default")))
unloadAgoraRtcEnginePlugin(agora::rtc::IRtcEngine *engine) {
    __android_log_print(ANDROID_LOG_DEBUG, "plugin", "plugin unloadAgoraRtcEnginePlugin");

    rtcEngine = nullptr;

    gCallBack = nullptr;

}
}


extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_Open(JNIEnv *env, jobject instance,
                                                                jstring url_) {

    const char *url = env->GetStringUTFChars(url_, 0);


    AgoraIPayerProxy::Get()->Open(url);
    AgoraIPayerProxy::Get()->Start();
    isComplete = true;
    env->ReleaseStringUTFChars(url_, url);
}

//receive audio data
void IPlayer::callBackData(XData data) {
    if(data.isAudio){
        XLOGI("audio data");
        s_audioFrameObserver.pushAudioData(data);
    }
}

//receive video data
void IPlayer::videoCallData(XData data) {

    AttachThreadScoped ats(gJVM);
    JNIEnv *jni_env = ats.env();
    XShaderType  type = (XShaderType)data.format;
    switch(type){
        case XSHADER_NV12:
        {
            uint8_t y[data.width * data.height];
            uint8_t u[data.width*data.height/4];
            uint8_t v[data.width*data.height/4];
            uint8_t  *i420Data = (uint8_t *)malloc(data.width*data.height*3/2);
            libyuv::NV12ToI420(data.datas[0],data.width,data.datas[1],data.width,y,data.width,u,data.width>>1,v,data.width>>1,data.width,data.height);
            jbyteArray retArray = jni_env->NewByteArray(3 * data.width * data.height / 2);
            jni_env->SetByteArrayRegion(retArray, 0, data.width * data.height, (jbyte*)y);
            jni_env->SetByteArrayRegion(retArray, data.width * data.height, data.width * data.height / 4, (jbyte*) u);
            jni_env->SetByteArrayRegion(retArray, 5 * data.width * data.height / 4, data.width * data.height / 4, (jbyte*)v);
            jni_env->CallVoidMethod(gCallBack,setExternalVideoFrameID,retArray,data.width,data.height);
            jni_env->DeleteLocalRef(retArray);
            free(i420Data);
            break;
        }
        case XSHADER_YUV420P:
        {
            jbyteArray retArray = jni_env->NewByteArray(3 * data.width * data.height / 2);
            jni_env->SetByteArrayRegion(retArray, 0, data.width * data.height, (jbyte*)data.datas[0]);
            jni_env->SetByteArrayRegion(retArray, data.width * data.height, data.width * data.height / 4, (jbyte*) data.datas[1]);
            jni_env->SetByteArrayRegion(retArray, 5 * data.width * data.height / 4, data.width * data.height / 4, (jbyte*)data.datas[2]);
            jni_env->CallVoidMethod(gCallBack,setExternalVideoFrameID,retArray,data.width,data.height);
            jni_env->DeleteLocalRef(retArray);
            break;
        }
        default:
            XLOGE("############################");
            break;

    }

}
//播放时长回调
void IPlayer::totalMsCallBack(int time) {

    AttachThreadScoped ats(gJVM);
    JNIEnv *jni_env = ats.env();
    jni_env->CallVoidMethod(gCallBack,totalTimeID,time);
    totalTime = time;

}
void IPlayer::ptsCallBack(int pts) {
    double num = pts/(double)totalTime;
    if(isComplete && num > 0.8){
//        XLOGD("当前的进度为%d",pts);
        AttachThreadScoped ats(gJVM);
        JNIEnv *jni_env = ats.env();
        jni_env->CallVoidMethod(gCallBack,videoCompleteID);
        isComplete = false;
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_XPlay_InitView(JNIEnv *env, jobject instance,
                                                             jobject surface) {

    //TODO
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    AgoraIPayerProxy::Get()->InitView(win);


}



extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_PlayOrPause(JNIEnv *env,
                                                                      jobject instance) {

    // TODO
    AgoraIPayerProxy::Get()->SetPause(!AgoraIPayerProxy::Get()->IsPause());

}
// extern "C"
//JNIEXPORT void JNICALL
//Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_setCallBack(JNIEnv *env,
//                                                       jobject instance) {
//    env->GetJavaVM(&gJVM);
//    // TODO
//    if (gCallBack == nullptr) {
//        gCallBack = env->NewGlobalRef(instance);
//        gCallbackClass = env->FindClass("io/agora/agoraplayerdemo/ui/LiveRoomActivity");
//        setExternalVideoFrameID = env->GetMethodID(gCallbackClass,"renderVideoFrame","([BII)V");
//    }
//}
extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_ChangeAudio(JNIEnv *env, jobject instance) {

    AgoraIPayerProxy::Get()->ChangeAudio(!AgoraIPayerProxy::Get()->isChangeAudioStream);

}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_XPlay_InitView(JNIEnv *env, jobject instance, jobject surface) {

    //TODO
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    AgoraIPayerProxy::Get()->InitView(win);

}

extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_initAgoraObserver(JNIEnv *env, jobject instance) {

    // TODO
//    XLOGE("FFDemux open success");


    agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
    mediaEngine.queryInterface(rtcEngine, agora::INTERFACE_ID_TYPE::AGORA_IID_MEDIA_ENGINE);
    if (mediaEngine) {

//        mediaEngine->registerVideoFrameObserver(&s_videoFrameObserver);
        mediaEngine->registerAudioFrameObserver(&s_audioFrameObserver);

    }
}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_setCallBack(JNIEnv *env, jobject instance) {

    env->GetJavaVM(&gJVM);
    // TODO
    if (gCallBack == nullptr) {
        gCallBack = env->NewGlobalRef(instance);
        gCallbackClass = env->FindClass("io/agora/agoraplayerdemo/ui/LiveRoomActivity");
        setExternalVideoFrameID = env->GetMethodID(gCallbackClass,"renderVideoFrame","([BII)V");
        videoCompleteID = env->GetMethodID(gCallbackClass,"videoComplete","()V");
        totalTimeID = env->GetMethodID(gCallbackClass,"totalMSCallBack","(I)V");
    }
}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_voiceSeek(JNIEnv *env, jobject instance,
                                                            jdouble pos) {

    // TODO

    s_audioFrameObserver.audio_voiceSum = pos;


}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_songSeek(JNIEnv *env, jobject instance,
                                                           jdouble pos) {

    // TODO

    s_audioFrameObserver.audio_sonSum = pos;
    AgoraIPayerProxy::Get()->SetPlayVolume(1 - pos);



}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_closeSong(JNIEnv *env, jobject instance) {

    // TODO
    AgoraIPayerProxy::Get()->Stop();
    AgoraIPayerProxy::Get()->Close();

}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_destroyAudioBuf(JNIEnv *env, jobject instance) {

    // TODO
    agoraAudioBuf.release();
    agoraAudioBuf.reset(new AudioCircularBuffer<char>(2048,true));

}extern "C"
JNIEXPORT jdouble JNICALL
Java_io_agora_agoraplayerdemo_ui_LiveRoomActivity_PlayPos(JNIEnv *env, jobject instance) {

    // TODO
    return AgoraIPayerProxy::Get()->PlayPos();

}