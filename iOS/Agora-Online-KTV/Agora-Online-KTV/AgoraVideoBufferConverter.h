//
//  AgoraVideoBufferConverter.h
//  AgoraPremium
//
//  Created by GongYuhua on 2017/12/4.
//  Copyright © 2017年 Agora. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
typedef NS_ENUM(NSUInteger, AgoraVideoPixelFormat) {
    AgoraVideoPixelFormatI420   = 1,
    AgoraVideoPixelFormatBGRA   = 2,
    AgoraVideoPixelFormatNV12   = 8,
};

typedef NS_ENUM(NSInteger, AgoraVideoRotation) {
    AgoraVideoRotationNone      = 0,
    AgoraVideoRotation90        = 1,
    AgoraVideoRotation180       = 2,
    AgoraVideoRotation270       = 3,
};

typedef NS_ENUM(NSInteger, AgoraVideoBufferType) {
    AgoraVideoBufferTypePixelBuffer = 1,
    AgoraVideoBufferTypeRawData     = 2,
};



@interface AgoraVideoBufferConverter : NSObject
+ (CVPixelBufferRef _Nonnull)convertRawData:(void * _Nonnull)rawData toNV12OrBGRAPixelBufferWithFormat:(AgoraVideoPixelFormat)format size:(CGSize)size;

+ (CVPixelBufferRef _Nonnull)convertToNV12FromI420PixelBuffer:(CVPixelBufferRef _Nonnull)buffer;
@end
