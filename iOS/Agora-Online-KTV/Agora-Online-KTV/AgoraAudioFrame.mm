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
//extern "C"{
//#import "scale.h"
//#import "rotate.h"
//#import "convert.h"
//}
static NSObject *lock = [[NSObject alloc] init];
//FILE* readPcmPointer;
//FILE* writePcmPointer;
static NSObject *threadLockCapture = [[NSObject alloc] init];
static NSObject *threadLockPlay = [[NSObject alloc] init];

using namespace std;
//using namespace libyuv;
FILE *readPcm ;

//class AgoraVideoFrameObserver:public agora::media::IVideoFrameObserver
//{
//
//public:
//    int yuvSize;
//    int width;
//    int height;
//    queue<void *>video;
//    void pushVideoFrame(void *yuv,int width,int height)
//    {
//        @synchronized(threadLockPlay) {
//            video.push(yuv);
//            NSLog(@"width * height ----- %d",width * height);
//            this->width = width;
//            this->height = height;
//            yuvSize = 360 * 640 * 1.5;
//        }
//    }
//    bool onCaptureVideoFrame(VideoFrame& videoFrame){
//
//        @synchronized(threadLockPlay) {
//
//            if (!video.empty()) {
//
//                const int src_w = 640;
//                const int src_h = 360;
//
//                const int dst_w = 90;
//                const int dst_h = 90;
//
//
//                const int d_yStride = 90;
//                const int d_uStride = 45;
//                const int d_vStride = 45;
//                //创建头像buf将数据传入buf中
//
//                uint8 *d_ybuf = (uint8 *)malloc(src_w * src_h);
//                uint8 *d_ubuf = (uint8 *)malloc(src_w * src_h /4);
//                uint8 *d_vbuf = (uint8 *)malloc(src_w * src_h /4);
//
//                memcpy(d_ybuf, videoFrame.yBuffer, src_w * src_h);
//                memcpy(d_ubuf, videoFrame.uBuffer, src_w * src_h/4);
//                memcpy(d_vbuf, videoFrame.vBuffer, src_w * src_h/4);
//
//                //视频画面的放缩
//                libyuv::I420Scale((uint8 *)videoFrame.yBuffer, videoFrame.yStride, (uint8 *)videoFrame.uBuffer, videoFrame.uStride, (uint8 *)videoFrame.vBuffer, videoFrame.vStride, src_w, src_h, d_ybuf, d_yStride, d_ubuf, d_uStride, d_vbuf, d_vStride, dst_w, dst_h, kFilterBox);
//
//                uint8 *r_ybuf = (uint8 *)malloc(dst_w * dst_h);
//                uint8 *r_ubuf = (uint8 *)malloc(dst_w * dst_h /4);
//                uint8 *r_vbuf = (uint8 *)malloc(dst_w * dst_h /4);
//                 //图像的旋转操作
//                libyuv::I420Rotate(d_ybuf, d_yStride, d_ubuf, d_uStride, d_vbuf, d_vStride, r_ybuf, d_yStride, r_ubuf, d_uStride, r_vbuf, d_vStride, dst_h, dst_w ,kRotate90);
////
//                //将电影数据放入缓存中
//                void *data = video.front();
//                char *tmpBuf = (char *)malloc(yuvSize);
//                memcpy(tmpBuf, data, yuvSize);
//                int ysize = this->width * this ->height;
//                char *ybuf = (char *)malloc(ysize);
//                memcpy(ybuf, tmpBuf, ysize);
//                int usize =this->width * this->height / 4;
//                char *ubuf = (char *)malloc(usize);
//                memcpy(ubuf, tmpBuf + this->width * this->height, usize);
//                char *vbuf = (char *)malloc(usize);
//                memcpy(vbuf, tmpBuf + this->width * this->height*5/4, usize);
//
//
////
//                //图像数据的叠加操作
//                int off_x = 640 - 25 - 90;
//                int off_y = 25;
//                int n0ff = 0;
//                for (int i = 0; i < dst_h; i++) {
//                    n0ff = src_w * (off_y + i) + off_x;
//                    memcpy(ybuf + n0ff, r_ybuf + dst_w * i, dst_w);
//                }
//                for (int j = 0; j < dst_h/2; j++) {
//                    n0ff = (src_w/2) * (off_y/2 + j) + off_x/2;
//                    memcpy(ubuf + n0ff, r_ubuf + dst_w/2 * j, dst_w/2);
//                    memcpy(vbuf + n0ff, r_vbuf + dst_w/2 * j, dst_w/2);
//                }
//
//
//                /* ---------------------转换完成之后图片替换的操作-----------------*/
//                videoFrame.width = this->width;
//                videoFrame.height = this->height;
//                videoFrame.yStride = this->width;
//                videoFrame.uStride = this->width/2;
//                videoFrame.vStride = this->width/2;
//
//                memcpy(videoFrame.yBuffer, ybuf, ysize);
//                memcpy(videoFrame.uBuffer, ubuf, usize);
//                memcpy(videoFrame.vBuffer, vbuf, usize);
//
//                free(tmpBuf);
//                free(ybuf);
//                free(ubuf);
//                free(vbuf);
//                free(r_ybuf);
//                free(r_ubuf);
//                free(r_vbuf);
//                free(d_ybuf);
//                free(d_ubuf);
//                free(d_vbuf);
//            }
//        }
//        return true;
//    }
//    int rotateYUV420Degree180(uint8_t* dstyuv,uint8_t* srcdata, int imageWidth, int imageHeight){
//        int i = 0, j = 0;
//        int index = 0;
//        int tempindex = 0;
//        int ustart = imageWidth; //*imageHeight;
//        tempindex= ustart;
//        for (i = 0; i <imageHeight; i++)
//        {
//            tempindex-= imageWidth;
//            for (j = 0; j <imageWidth; j++)
//            {
//                dstyuv[index++] = srcdata[tempindex + j];
//            }
//        }
//        int udiv = imageWidth *imageHeight / 4;
//        int uWidth = imageWidth /2;
//        int uHeight = imageHeight /2;
//        index= ustart; tempindex= ustart+udiv;
//        for (i = 0; i < uHeight;i++)
//        { tempindex-= uWidth;
//            for (j = 0; j < uWidth;j++)
//            {
//                dstyuv[index]= srcdata[tempindex + j];
//                dstyuv[index+ udiv] = srcdata[tempindex + j + udiv];
//                index++;
//            }
//
//        } return 0;
//
//    }
//
//     bool onRenderVideoFrame(unsigned int uid, VideoFrame& videoFrame){
//
//         return true;
//    }
//};

class AgoraAudioFrameObserver:public agora::media::IAudioFrameObserver
{
    // total buffer length of per second
    enum { kBufferLengthBytes = 100000000 }; // 88200 bytes
    // capture
    char byteBuffer[kBufferLengthBytes]; // char take up 1 byte, byterBuffer[] take up 88200 bytes
    queue<char *>tmp;
    queue<long>length;
    int readIndex = 0;
    int writeIndex = 0;
    int availableBytes = 0;
    int channels = 2;
    int bufferLength = 2048;
    

    // play
    char byteBuffer_play[kBufferLengthBytes];
    int readIndex_play = 0;
    int writeIndex_play = 0;
    int availableBytes_play = 0;
    int channels_play = 1;
    
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
            int frameSize = bytesLength;
            int remainedSize = kAudioBufferPoolSize - mRecordingAppBufferBytes;
            if (remainedSize >= frameSize) {
                memcpy(mRecordingAudioAppPool+mRecordingAppBufferBytes, data, frameSize);
            } else {
                mRecordingAppBufferBytes = 0;
                memcpy(mRecordingAudioAppPool+mRecordingAppBufferBytes, data, frameSize);
            }
            
            mRecordingAppBufferBytes += frameSize;
            //混入SDK播放端的音频
            
//            int playRemainSize = kAudioBufferPoolSize - mPlayAppBufferBytes;
//            if (playRemainSize >= frameSize) {
//                memcpy(mPlayAudioAppPool + mPlayAppBufferBytes, data, frameSize);
//            }else {
//                mPlayAppBufferBytes = 0;
//                memcpy(mPlayAudioAppPool + mPlayAppBufferBytes, data, frameSize);
//            }
//            mPlayAppBufferBytes += frameSize;
//            

        }
        
        
    }
    virtual bool onRecordAudioFrame(AudioFrame& audioFrame) override
    {
        @synchronized(threadLockCapture) {
//            NSInteger time = [[NSDate date] timeIntervalSince1970];
//            NSLog(@"%ld",time);
            int bytes = audioFrame.samples * audioFrame.channels * audioFrame.bytesPerSample;

            if (mRecordingAppBufferBytes < bytes) {
                
                return true;
            }
            short *mixedBuffer = (short *)malloc(bytes);
            if (mRecordingAppBufferBytes >= bytes) {
                memcpy(mixedBuffer, mRecordingAudioAppPool, bytes);
                mRecordingAppBufferBytes -= bytes;
                memcpy(mRecordingAudioAppPool, mRecordingAudioAppPool+bytes, mRecordingAppBufferBytes);


            }else{
                NSLog(@" mRecordingAppBufferBytes %d",mRecordingAppBufferBytes);
            }
            short *tmpBuf = (short *)malloc(bytes);
            memcpy(tmpBuf, audioFrame.buffer, bytes);
            for (int i = 0 ; i < bytes/2; i++) {
                tmpBuf[i] += (mixedBuffer[i]*0.1);
            }
            memcpy(audioFrame.buffer, tmpBuf, bytes);
            free(mixedBuffer);

        }
            return true;
        
    }
    virtual bool onPlaybackAudioFrame(AudioFrame& audioFrame) override{
        @synchronized(threadLockPlay){
            int bytes = audioFrame.samples * audioFrame.channels * audioFrame.bytesPerSample;
            if (mPlayAppBufferBytes < bytes) {
                return true;
            }
            short *mixedBuffer = (short *)malloc(bytes);
            if (mPlayAppBufferBytes >= bytes) {
                memcpy(mixedBuffer, mPlayAudioAppPool, bytes);
                mPlayAppBufferBytes -= bytes;
                memcpy(mPlayAudioAppPool, mPlayAudioAppPool + bytes, mPlayAppBufferBytes);
            }else{
                NSLog(@"mPlayAppBufferbytes %d",mPlayAppBufferBytes);
            }
            short *tmpBuf = (short *)malloc(bytes);
            memcpy(tmpBuf, audioFrame.buffer, bytes);
            for (int i  = 0; i < bytes /2 ; i++) {
                tmpBuf[i] += mixedBuffer[i];
            }
            memcpy(audioFrame.buffer, tmpBuf, bytes);
            free(mixedBuffer);
            free(tmpBuf);
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
//        s_videoFrameObserver = new AgoraVideoFrameObserver();
//        mediaEngine->registerVideoFrameObserver(s_videoFrameObserver);
//
    }
    
    
}
-(void)pushAudioSource:(void *)data byteLength:(long)bytesLength{
    s_audioFrameObserver->pushExternalData(data, bytesLength);
}
//-(void)pushVideoSource:(void *)yuv width:(int)width height:(int)height
//{
//    s_videoFrameObserver->pushVideoFrame(yuv, width, height);
//}

@end



