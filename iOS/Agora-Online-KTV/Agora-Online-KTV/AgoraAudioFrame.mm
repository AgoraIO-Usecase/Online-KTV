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

#import "SuperpoweredAdvancedAudioPlayer.h"
#import "SuperpoweredReverb.h"
#import "SuperpoweredFilter.h"
#import "Superpowered3BandEQ.h"
#import "SuperpoweredEcho.h"
#import "SuperpoweredRoll.h"
#import "SuperpoweredFlanger.h"
#import "SuperpoweredSimple.h"
#import <mach/mach_time.h>
#import "AudioCircularBuffer.h"
using namespace AgoraRTC;
static scoped_ptr<AudioCircularBuffer<char>> agoraAudioBuf(new AudioCircularBuffer<char>(2048,true));
static scoped_ptr<AudioCircularBuffer<short>> earBackBuf(new AudioCircularBuffer<int16_t>(2048,true));

static NSObject *lock = [[NSObject alloc] init];
static NSObject *threadLockCapture = [[NSObject alloc] init];
static NSObject *threadLockPlay = [[NSObject alloc] init];
static NSObject *threadLockPush = [[NSObject alloc] init];

using namespace std;
static SuperpoweredFX *effects[NUMFXUNITS];
static float *stereoBuffer;
//static float *effectBuffer;

static float audio_sonSum = 1;
static float audio_voiceSum = 1;

class AgoraAudioFrameObserver:public agora::media::IAudioFrameObserver
{
    
public:
    
    int sampleRate = 0;
    int sampleRate_play = 0;
    int isOpenAudioEffect;
    float voiceSum;
    float songSum;
    
public:
#pragma mark- <C++ Capture>
    // push audio data to special buffer(Array byteBuffer)
    // bytesLength = date length
    void pushExternalData(void* data, long bytesLength)
    {
        @synchronized(threadLockPush) {
//            //采集端的音频
            char  *buf = (char *)malloc((int)bytesLength);
            memcpy(buf, data, bytesLength);
            agoraAudioBuf->Push(buf, (int)bytesLength);
            free(buf);
        }
        
        
    }
    virtual bool onRecordAudioFrame(AudioFrame& audioFrame) override
    {
        @synchronized(threadLockCapture) {
            
            int bytes = audioFrame.samples * audioFrame.channels * audioFrame.bytesPerSample;
            int16_t *tmpBuf = (int16_t *)malloc(sizeof(int16_t)*bytes);
            memcpy(tmpBuf, audioFrame.buffer, bytes);
//            for (int i = 0 ; i < bytes ; i++) {
//                stereoBuffer[i] = tmpBuf[i]/32767.0f;
//            }
////            effects[REVERBINDEX]->process(stereoBuffer,stereoBuffer,audioFrame.samples);
//
//            for (int i = 0; i < bytes ; i ++) {
//                int tmp = stereoBuffer[i] * 32767;
//                tmpBuf[i] = tmp ?  32767: tmp;
//                tmpBuf[i] = tmp < -32768 ? -32768: tmp;
//
//            }

//            if ([[AgoraAudioFrame shareInstance] isHeadsetPluggedIn]) {
//
//
//                earBackBuf->Push(tmpBuf, bytes);
//
//            }
            if (agoraAudioBuf->mAvailSamples < bytes) {
                memcpy(audioFrame.buffer, tmpBuf, sizeof(int16_t)*bytes);
                free(tmpBuf);
                return true;
            }
            char *data = (char *)malloc(sizeof(char)*bytes);
            agoraAudioBuf->Pop(data, bytes);
            int16_t *audioBuf = (int16_t *)malloc(sizeof(int16_t)*bytes);
            memcpy(audioBuf, tmpBuf, bytes);
            int16_t* p16 = (int16_t*) data;

            for (int i = 0; i < bytes / 2; ++i) {
                int tmp = p16[i] * audio_sonSum;
                audioBuf[i] = audioBuf[i] * audio_voiceSum;
                tmp += audioBuf[i];
                //防溢出的处理
                if (tmp > 32767) {
                    audioBuf[i] = 32767;
                }
                else if (tmp < -32768) {
                    audioBuf[i] = -32768;
                }
                else {
                    audioBuf[i] = tmp;
                }
            }
            memcpy(audioFrame.buffer, audioBuf,sizeof(int16_t) * bytes);
            free(audioBuf);
            free(tmpBuf);
            free(p16);
        }
        return true;
        
    }
    virtual bool onPlaybackAudioFrame(AudioFrame& audioFrame) override{
        @synchronized(threadLockPlay){
            int bytes = audioFrame.samples * audioFrame.channels * audioFrame.bytesPerSample;
//            //判断是否插入了耳机
//            ;
//            if (earBackBuf->mAvailSamples > bytes) {
//                if ([[AgoraAudioFrame shareInstance] isHeadsetPluggedIn]) {
//                    int16_t *tmpBuf = (int16_t *)malloc(sizeof(int16_t) * bytes);
//                    memcpy(tmpBuf, audioFrame.buffer, bytes);
//                    int16_t *earbuf = (int16_t *)malloc(sizeof(int16_t) * bytes);
//                    earBackBuf->Pop(earbuf,bytes);
//                    //做个判断
//                    for (int i = 0 ; i < bytes; i++) {
//
//                       int tmp  =   tmpBuf[i] + earbuf[i];
//                        if (tmp > 32767) {
//                            tmpBuf[i] = 32767;
//                        }else if(tmp < -32768){
//                            tmpBuf[i] = -32768;
//                        }else{
//                            tmpBuf[i] = tmp;
//                        }
//                    }
//                    memcpy(audioFrame.buffer, tmpBuf,sizeof(int16_t) * bytes);
//                    free(tmpBuf);
//                    free(earbuf);
//                }
//
//            }
            //            if ([AgoraAudioFrame shareInstance].isAudience) {
            //                int16_t audioBuf[bytes];
            //                memcpy(audioBuf, audioFrame.buffer, bytes);
            //                for (int i = 0 ; i < bytes ; i++) {
            //                    effectBuffer[i] = audioBuf[i]/32767.0f;
            //                }
            //                effects[EQINDEX]->process(effectBuffer,effectBuffer,audioFrame.samples);
            ////                effects[REVERBINDEX]->process(effectBuffer,effectBuffer,audioFrame.samples);
            //
            //                for (int i = 0; i < bytes ; i ++) {
            //                    int16_t tmp = effectBuffer[i] * 32767;
            //                    if (tmp > 32767) {
            //                        NSLog(@"32767");
            //                    }
            //                    if (tmp < -32768) {
            //                        NSLog(@"-32768");
            //                    }
            //                    audioBuf[i] = effectBuffer[i] > 32767 ?  32767: tmp;
            //                    audioBuf[i] = effectBuffer[i] < -32768 ? -32768: tmp;
            //                }
            //                memcpy(audioFrame.buffer, audioBuf, bytes);
            //            }
        }
        
        return true;
    }
    virtual  bool onPlaybackAudioFrameBeforeMixing(unsigned int uid, AudioFrame& audioFrame) override {
        
        return true;
    }
    virtual bool onMixedAudioFrame(AudioFrame& audioFrame) override {
        
        
        return true; }
    
};
@interface AgoraAudioFrame()
{
    AgoraRtcEngineKit *_rtcEngine;
}
@end
static AgoraAudioFrameObserver* s_audioFrameObserver;

@implementation AgoraAudioFrame

+ (instancetype)shareInstance{
    
    static dispatch_once_t once;
    static AgoraAudioFrame *sharedInstance;
    dispatch_once(&once, ^{
        if (sharedInstance == NULL) {
        
            sharedInstance = [[self alloc] init];
        }
    });
    return sharedInstance;
    
}

-(void)registerEngineKit:(AgoraRtcEngineKit *)rtcEngine
{
    _rtcEngine = rtcEngine;
    agora::rtc::IRtcEngine* rtc_engine = (agora::rtc::IRtcEngine*)rtcEngine.getNativeHandle;
    agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
    mediaEngine.queryInterface(rtc_engine, agora::AGORA_IID_MEDIA_ENGINE);
    if (mediaEngine) {
        s_audioFrameObserver = new AgoraAudioFrameObserver();
        s_audioFrameObserver->sampleRate = (int)self.sampleRate;
        s_audioFrameObserver->sampleRate_play = (int)self.sampleRate;
        mediaEngine->registerAudioFrameObserver(s_audioFrameObserver);
        
    }
    
    SuperpoweredEcho *delay = new SuperpoweredEcho((int)self.sampleRate);
    delay->setMix(0.5f);
    effects[DELAYINDEX] = delay;
    effects[DELAYINDEX]->enable(0);
    
    SuperpoweredReverb *reverb = new SuperpoweredReverb((int)self.sampleRate);
    reverb->setRoomSize(0.8f);
    reverb->setMix(0.3f);
    reverb->setPredelay(30);
    reverb->setDamp(0.5);
    reverb->setWidth(1.0f);
    effects[REVERBINDEX] = reverb;
    effects[REVERBINDEX]->enable(0);
    
    
    Superpowered3BandEQ *eq = new Superpowered3BandEQ((int)self.sampleRate);
    eq->bands[0] = 1.0f;
    eq->bands[1] = 0.5f;
    eq->bands[2] = 6.0f;
    effects[EQINDEX] = eq;
    effects[EQINDEX]->enable(1);
    
    if(posix_memalign((void **)&stereoBuffer, 16, 4096 + 128) != 0) abort(); // Allocating memory, aligned to 16.
}
-(void)pushAudioSource:(void *)data byteLength:(long)bytesLength{
    s_audioFrameObserver->pushExternalData(data, bytesLength);
}
- (BOOL)isHeadsetPluggedIn {
    @synchronized(self){
        AVAudioSessionRouteDescription* route = [[AVAudioSession sharedInstance] currentRoute];
        for (AVAudioSessionPortDescription* desc in [route outputs]) {
            if ([[desc portType] isEqualToString:AVAudioSessionPortHeadphones])
                return YES;
        }
    return false;
    }
}
-(void)setIsAudience:(BOOL)isAudience
{
    _isAudience = isAudience;
}
-(void)destroyAudioBuf{
    
        agoraAudioBuf.release();
        earBackBuf.release();
        agoraAudioBuf.reset(new AudioCircularBuffer<char>(2048,true));
        earBackBuf.reset(new AudioCircularBuffer<int16_t>(2048,true));
}
-(void)setSongNum:(float)songNum
{
    _songNum = songNum;
    audio_sonSum = songNum;
}
-(void)setVoiceNum:(float)voiceNum
{
    _voiceNum = voiceNum;
    audio_voiceSum = voiceNum;
}
-(void)destroy{
    agora::rtc::IRtcEngine* rtc_engine = (agora::rtc::IRtcEngine*)_rtcEngine.getNativeHandle;
    agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
    mediaEngine.queryInterface(rtc_engine, agora::AGORA_IID_MEDIA_ENGINE);
    if (mediaEngine) {
        mediaEngine->registerAudioFrameObserver(NULL);
    }
    agoraAudioBuf.release();
    earBackBuf.release();
}
@end





