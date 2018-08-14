//
//  AgoraAudioFrame.cpp
//  AgoraShareVideo
//
//  Created by 湛孝超 on 2018/3/9.
//  Copyright © 2018年 湛孝超. All rights reserved.
//

#import "AgoraAudioFrame.h"
#include <stdio.h>
#include <iostream>
#include <queue>
#include <AgoraRtcEngineKit/IAgoraRtcEngine.h>
#include <AgoraRtcEngineKit/IAgoraMediaEngine.h>

static NSObject *lock = [[NSObject alloc] init];
//FILE* readPcmPointer;
//FILE* writePcmPointer;
static NSObject *threadLockCapture = [[NSObject alloc] init];
static NSObject *threadLockPlay = [[NSObject alloc] init];

using namespace std;
//using namespace libyuv;
FILE *readPcm ;
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
            cout << "取值越界" << endl;
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
class AgoraAudioFrameObserver:public agora::media::IAudioFrameObserver
{
    
public:
    int sampleRate = 0;
    int sampleRate_play = 0;
public:
#pragma mark- <C++ Capture>
    // push audio data to special buffer(Array byteBuffer)
    // bytesLength = date length
    void pushExternalData(void* data, long bytesLength)
    {
        @synchronized(threadLockCapture) {
//            if(!readPcm) {
//                NSArray  *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
//                NSString *documentsDirectory = [paths objectAtIndex:0];
//                NSString* pcmPath;
//                pcmPath = [NSString stringWithFormat: @"%@/file_in.pcm", documentsDirectory];
//                readPcm = fopen([pcmPath UTF8String], "wb");
//            }
//           fwrite(data, 2, bytesLength*2, readPcm);
//              NSInteger time = [[NSDate date] timeIntervalSince1970];
//              NSLog(@"%ld",time);
//
            //采集端的音频
            char audioBuf[bytesLength];
            memcpy(audioBuf, data, bytesLength);
            audioPool.add_elements(audioBuf, (int)bytesLength);
        }
        
        
    }
    virtual bool onRecordAudioFrame(AudioFrame& audioFrame) override
    {
        @synchronized(threadLockCapture) {
            
            int bytes = audioFrame.samples * audioFrame.channels * audioFrame.bytesPerSample;
            if (audioPool.finish < bytes) {
                
                return true;
            }
            short *mixedBuffer = (short *)malloc(bytes);
            char *data = audioPool.pop_elements((int)bytes);
            memcpy(mixedBuffer, data, bytes);
            short *tmpBuf = (short *)malloc(bytes);
            memcpy(tmpBuf, audioFrame.buffer, bytes);
            for (int i = 0 ; i < bytes/2; i++) {
                
                tmpBuf[i] += (mixedBuffer[i] * 0.5);
            }
            memcpy(audioFrame.buffer, tmpBuf, bytes);
            free(mixedBuffer);
            free(tmpBuf);
            free(data);
        }
            return true;
        
    }
    virtual bool onPlaybackAudioFrame(AudioFrame& audioFrame) override{
        @synchronized(threadLockPlay){

        }
   
        return true;
    }
    virtual  bool onPlaybackAudioFrameBeforeMixing(unsigned int uid, AudioFrame& audioFrame) override {
        
        return true;
    }
    virtual bool onMixedAudioFrame(AudioFrame& audioFrame) override {
        
    
        return true; }
    
};
static AgoraAudioFrameObserver* s_audioFrameObserver;
@implementation AgoraAudioFrame

+ (instancetype)shareInstance{
    
    static dispatch_once_t once;
    static AgoraAudioFrame *sharedInstance;
    dispatch_once(&once, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
    
}

-(void)registerEngineKit:(AgoraRtcEngineKit *)rtcEngine
{
    
    agora::rtc::IRtcEngine* rtc_engine = (agora::rtc::IRtcEngine*)rtcEngine.getNativeHandle;
    agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
    mediaEngine.queryInterface(rtc_engine, agora::AGORA_IID_MEDIA_ENGINE);
    if (mediaEngine) {
        s_audioFrameObserver = new AgoraAudioFrameObserver();
        s_audioFrameObserver->sampleRate = 16000;
        s_audioFrameObserver->sampleRate_play = 16000;
        mediaEngine->registerAudioFrameObserver(s_audioFrameObserver);

    }
    
    
}
-(void)pushAudioSource:(void *)data byteLength:(long)bytesLength{
    s_audioFrameObserver->pushExternalData(data, bytesLength);
}

@end



