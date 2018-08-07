//
//  AgoraMixVideo.h
//  Agora-Online-KTV
//
//  Created by zhanxiaochao on 2018/8/1.
//  Copyright © 2018年 湛孝超. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
@interface AgoraMixVideo : NSObject

//摄像头数据和mv数据的切换
@property (nonatomic, assign)BOOL captureAndMV;
//关闭摄像头数据采集
@property (nonatomic, assign)BOOL isOpenCapture;


@property (copy) void (^onVideoCallBack)(uint8_t *  buf ,size_t yStride,size_t localWidth,size_t localHeight);
@property (copy) void (^onVideoDataCallBack)(CVPixelBufferRef buf);
@end
