//
//  AgoraAudioFrame.hpp
//  AgoraShareVideo
//
//  Created by 湛孝超 on 2018/3/9.
//  Copyright © 2018年 湛孝超. All rights reserved.
//
#import <AgoraRtcEngineKit/AgoraRtcEngineKit.h>

static const int kAudioBufferPoolSize = 163840 * 2 * 2;
static  unsigned char mRecordingAudioAppPool[kAudioBufferPoolSize];
static int mRecordingAppBufferBytes = 0;
static int mRecordingMicBufferBytes = 0;
static  unsigned char mRecordingAudioMicPool[kAudioBufferPoolSize];
@interface AgoraAudioFrame:NSObject
typedef struct AVideoFrame {
    
    uint8_t *data[8];    ///< pointers to the image data planes
    int linesize[8];   ///< number of bytes per line
} AVideoFrame;

+(instancetype)shareInstance;
-(void)registerEngineKit:(AgoraRtcEngineKit *)rtcEngine;
-(void)pushAudioSource:(void *)data byteLength:(long)bytesLength;
-(void)pushVideoSource:(void *)yuv width:(int)width height:(int)height;
@end

