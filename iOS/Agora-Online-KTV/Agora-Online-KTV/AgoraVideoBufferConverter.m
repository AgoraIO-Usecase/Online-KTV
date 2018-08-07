//
//  AgoraVideoBufferConverter.m
//  AgoraPremium
//
//  Created by GongYuhua on 2017/12/4.
//  Copyright © 2017年 Agora. All rights reserved.
//

#import "AgoraVideoBufferConverter.h"
#import "libyuv.h"

@implementation AgoraVideoBufferConverter
+ (CVPixelBufferRef _Nonnull)convertRawData:(void* _Nonnull)rawData
          toNV12OrBGRAPixelBufferWithFormat:(AgoraVideoPixelFormat)format
                                       size:(CGSize)size {
    int width = size.width;
    int height = size.height;
    
    CFStringRef key[1];
    key[0] = kCVPixelBufferIOSurfacePropertiesKey;
    CFDictionaryRef value[1];
    value[0] = CFDictionaryCreate(NULL, NULL, NULL, 0, &kCFCopyStringDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    CFDictionaryRef attribut_dict;
    attribut_dict = CFDictionaryCreate(NULL, (const void **)key, (const void **)value, 1, &kCFCopyStringDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    
    CVPixelBufferRef newBuffer;
    OSType type = format == AgoraVideoPixelFormatBGRA ? kCVPixelFormatType_32BGRA : kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange;
    CVPixelBufferCreate(NULL, width, height, type, attribut_dict, &newBuffer);
    CFRelease(attribut_dict);
    CFRelease(value[0]);
    CVPixelBufferLockBaseAddress(newBuffer, 0);
    
    uint8_t* dstY = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(newBuffer, 0);
    size_t strideY = CVPixelBufferGetBytesPerRowOfPlane(newBuffer, 0);
    
    if (strideY < width) {
        CVPixelBufferUnlockBaseAddress(newBuffer, 0);
        return newBuffer;
    }
    
    int area = width * height;
    
    switch (format) {
        case AgoraVideoPixelFormatBGRA: {
            if (strideY == width) {
                memcpy(dstY, rawData, area * 4);
            } else {
                for (int i = 0; i < height; i++) {
                    memcpy(dstY + strideY * i, rawData + width * i * 4, width * 4);
                }
            }
            break;
        }
        case AgoraVideoPixelFormatI420: {
            uint8_t* dstU = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(newBuffer, 1);
            size_t strideU = CVPixelBufferGetBytesPerRowOfPlane(newBuffer, 1);
            I420ToNV12(rawData, width,
                       rawData + area, width / 2,
                       rawData + area * 5 / 4, width / 2,
                       dstY, (int)strideY,
                       dstU, (int)strideU,
                       width, height);
            break;
        }
        case AgoraVideoPixelFormatNV12: {
            uint8_t* dstUV = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(newBuffer, 1);
            int strideUV = (int)CVPixelBufferGetBytesPerRowOfPlane(newBuffer, 1);
            if (strideY == width) {
                memcpy(dstY, rawData, area);
                memcpy(dstUV, rawData + area, area / 2);
            } else {
                for (int i = 0; i < height; i++) {
                    memcpy(dstY + strideY * i, rawData + width * i, width);
                }
                for (int i = 0; i < height / 2; i++) {
                    memcpy(dstUV + strideUV * i, rawData + area + width * i, width);
                }
            }
            break;
        }
    }
    
    CVPixelBufferUnlockBaseAddress(newBuffer, 0);
    return newBuffer;
}

+ (CVPixelBufferRef _Nonnull)convertToNV12FromI420PixelBuffer:(CVPixelBufferRef _Nonnull)buffer {
    CVPixelBufferLockBaseAddress(buffer, 0);
    
    Boolean isPlanar = CVPixelBufferIsPlanar(buffer);
    size_t width = isPlanar ? CVPixelBufferGetWidthOfPlane(buffer, 0) : CVPixelBufferGetWidth(buffer);
    size_t height = isPlanar ? CVPixelBufferGetHeightOfPlane(buffer, 0) : CVPixelBufferGetHeight(buffer);
    
    size_t stride0 = CVPixelBufferGetBytesPerRowOfPlane(buffer, 0);
    size_t stride1 = CVPixelBufferGetBytesPerRowOfPlane(buffer, 1);
    size_t stride2 = CVPixelBufferGetBytesPerRowOfPlane(buffer, 1);
    
    uint8_t* srcY = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(buffer, 0);
    uint8_t* srcU = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(buffer, 1);
    uint8_t* srcV = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(buffer, 2);
    
    CFStringRef key[1];
    key[0] = kCVPixelBufferIOSurfacePropertiesKey;
    CFDictionaryRef value[1];
    value[0] = CFDictionaryCreate(NULL, NULL, NULL, 0, &kCFCopyStringDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    CFDictionaryRef attribut_dict;
    attribut_dict = CFDictionaryCreate(NULL, (const void **)key, (const void **)value, 1, &kCFCopyStringDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    
    CVPixelBufferRef newBuffer;
    CVPixelBufferCreate(NULL, width, height, kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, attribut_dict, &newBuffer);
    CFRelease(attribut_dict);
    CFRelease(value[0]);
    CVPixelBufferLockBaseAddress(newBuffer, 0);
    
    uint8_t* dstY = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(newBuffer, 0);
    size_t strideY = CVPixelBufferGetBytesPerRowOfPlane(newBuffer, 0);
    uint8_t* dstUV = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(newBuffer, 1);
    size_t strideUV = CVPixelBufferGetBytesPerRowOfPlane(newBuffer, 1);
    I420ToNV12(srcY, (int)stride0,
               srcU, (int)stride1,
               srcV, (int)stride2,
               dstY, (int)strideY,
               dstUV, (int)strideUV,
               (int)width, (int)height);
    
    CVPixelBufferUnlockBaseAddress(newBuffer, 0);
    CVPixelBufferUnlockBaseAddress(buffer, 0);
    
    return newBuffer;
}
@end
