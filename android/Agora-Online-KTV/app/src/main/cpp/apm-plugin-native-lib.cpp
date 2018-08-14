#include <jni.h>
#include <string>
#include "AgoraIPayerProxy.h"
#include "AgoraLog.h"
#include <android/native_window_jni.h>
#include "IAgoraMediaEngine.h"
#include "IAgoraRtcEngine.h"
#include "VMUtil .h"
#include "XShader.h"
#include <iostream>
jobject gCallBack = nullptr;
jclass gCallbackClass = nullptr;
jmethodID setExternalVideoFrameID = nullptr;
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

static const int kAudioBufferPoolSize = 163840 * 2 * 2;
static unsigned char mRecordingAudioAppPool[kAudioBufferPoolSize];
static int mRecordingAppBufferBytes = 0;

template <typename  Ty>
#define  STEP_SIZE (8192)
class AudioPoolBuffer
{
public:

    typedef Ty value;
    typedef Ty *viter;
public:
    AudioPoolBuffer(int nLen = 0):m_nLen(nLen),m_data(NULL),finish(0){
        if(nLen > 0){
            m_data = (Ty *)malloc(sizeof(Ty)*nLen);
            star = m_data;
            dataSize = nLen;
        }
    }
    AudioPoolBuffer(){
        free(m_data);
    }
    void push_back(const value & x){
        if(dataSize > finish){
            *(star + finish) = x;
            ++finish;
        } else{
            adjustSize = adjust_size(dataSize + STEP_SIZE);
            m_data = (Ty *)realloc(m_data,sizeof(Ty)*adjustSize);
            star = m_data;
            dataSize = adjustSize;
            *(star + finish) = x;
            ++finish;
        }
    }
    void add_elements(const value * x,int size){
        for (int i = 0; i < size; ++i) {
            push_back(x[i]);
        }
    }
    inline  value pop_back(){
        --finish;
        return *(star + finish);
    }
    value operator[](int  n){
        if(n == finish || n < finish){
            return *(star+ n);
        }else{
            std::cout << "取值越界" << std::endl;
        }
    }
    int adjust_size(int size)
    {
        size += (STEP_SIZE - 1);
        size /= STEP_SIZE;
        size *= STEP_SIZE;
        return size;
    }
    value * pop_elements(int size){
        value *arr = (value *)malloc(size);
        memcpy(arr, star, size);
        finish -= size;
        memmove(star, (star + size), finish);
        return  arr;
    }

public:
    int finish;
protected:
    viter m_data;
    int m_nLen;
    viter star;
    int dataSize;//
    int adjustSize;
};
static  AudioPoolBuffer<char> audioPool(2048);


class AgoraAudioFrameObserver : public agora::media::IAudioFrameObserver {

public:
    AgoraAudioFrameObserver() {
        gCallBack = nullptr;
    }

    ~AgoraAudioFrameObserver() {
    }


public:

    void pushAudioData(XData data){
        mux.lock();
//        if(!readPcmPointer) {
//            char *path = "/sdcard/file_in_7.pcm";
//            readPcmPointer = fopen(path, "wb");
//        }
//        fwrite(data.data, 2, data.size, readPcmPointer);

        //采集端的音频
        char audioBuf[data.size];
        memcpy(audioBuf, data.data, data.size);
        audioPool.add_elements(audioBuf, (int)data.size);
        mux.unlock();

    }
    virtual bool onRecordAudioFrame(AudioFrame &audioFrame) override {
        mux.lock();
        //回调数据
        int16  bytes = audioFrame.samples * audioFrame.channels * audioFrame.bytesPerSample;

        if (audioPool.finish < bytes) {
            mux.unlock();
            return true;
        }
        int16 *mixedBuffer = (int16 *)malloc(bytes);
        char *data = audioPool.pop_elements((int)bytes);
        memcpy(mixedBuffer, data, bytes);

        int16 *tmpBuf = (int16 *)malloc((int16)bytes);
        memcpy(tmpBuf, audioFrame.buffer, (int16)bytes);
        for (int i = 0 ; i < (int16)bytes/2; i++) {
            tmpBuf[i] += (mixedBuffer[i] * 0.4);//修改
        }
        memcpy(audioFrame.buffer, tmpBuf,(int16)bytes);
        free(mixedBuffer);
        free(tmpBuf);
        free(data);
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
JNIEXPORT jstring
JNICALL
Java_io_agora_agoraplayerdemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {

    std::string hello = "Hello from C++";
//
//    XLOGE("FFDemux open success");


    agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
    mediaEngine.queryInterface(rtcEngine, agora::INTERFACE_ID_TYPE::AGORA_IID_MEDIA_ENGINE);
    if (mediaEngine) {

//        mediaEngine->registerVideoFrameObserver(&s_videoFrameObserver);
        mediaEngine->registerAudioFrameObserver(&s_audioFrameObserver);

    }

    return env->NewStringUTF(hello.c_str());

}
typedef int (*Callback) (XData data);
void AudioCallBackData(XData data){

};
extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_MainActivity_Open(JNIEnv *env, jobject instance,
                                                                jstring url_) {

    const char *url = env->GetStringUTFChars(url_, 0);
    AgoraIPayerProxy::Get()->Open(url);
    AgoraIPayerProxy::Get()->Start();
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
Java_io_agora_agoraplayerdemo_MainActivity_PlayOrPause(JNIEnv *env,
                                                                      jobject instance) {

    // TODO
    AgoraIPayerProxy::Get()->SetPause(!AgoraIPayerProxy::Get()->IsPause());

}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_MainActivity_setCallBack(JNIEnv *env,
                                                       jobject instance) {
    env->GetJavaVM(&gJVM);
    // TODO
    if (gCallBack == nullptr) {
        gCallBack = env->NewGlobalRef(instance);
        gCallbackClass = env->FindClass("io/agora/agoraplayerdemo/MainActivity");
        setExternalVideoFrameID = env->GetMethodID(gCallbackClass,"renderVideoFrame","([BII)V");
    }
}extern "C"
JNIEXPORT void JNICALL
Java_io_agora_agoraplayerdemo_MainActivity_ChangeAudio(JNIEnv *env, jobject instance) {

    AgoraIPayerProxy::Get()->ChangeAudio(!AgoraIPayerProxy::Get()->isChangeAudioStream);

}