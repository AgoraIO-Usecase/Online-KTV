//
//  AgoraVideoCapture.m
//  Agora-Online-KTV
//
//  Created by zhanxiaochao on 2018/8/1.
//  Copyright © 2018年 湛孝超. All rights reserved.
//

#import "AgoraVideoCapture.h"
@interface AgoraVideoCapture()<AVCaptureVideoDataOutputSampleBufferDelegate>
@property(nonatomic, strong) AVCaptureSession *captureSession;
@end
@implementation AgoraVideoCapture
-(instancetype)init
{
    if (self = [super init]) {
        [self initVideoCapture];
    }
    return self;
}
-(void)initVideoCapture{
    
    // 1. 创建捕获会话：设置分辨率
    [self setupSession];
    
    // 2. 添加输入
    [self setupInput];
    
    // 3. 添加输出
    [self setupOutput];
    
    // 开启会话
    // 一开启会话，就会在输入与输出对象之间建立起连接
    [_captureSession startRunning];
}
-(void)setupSession{
    
    AVCaptureSession *captureSession = [[AVCaptureSession alloc] init];
    _captureSession = captureSession;
    captureSession.sessionPreset = AVCaptureSessionPreset640x480;
}
-(void)setupInput{
    AVCaptureDevice *videoDevice = [self deviceWithPosition:AVCaptureDevicePositionFront];
//    videoDevice.activeVideoMinFrameDuration = CMTimeMake(CACurrentMediaTime()*1000, 1000);
    //设备输入对象
    AVCaptureDeviceInput *videoInput =  [AVCaptureDeviceInput deviceInputWithDevice:videoDevice error:nil];
    // 给会话添加输入
    if ([_captureSession canAddInput:videoInput]) {
        [_captureSession addInput:videoInput];
    }
    AVCaptureDeviceFormat *bestFormat = nil;
    AVFrameRateRange *bestFrameRateRange = nil;
    for ( AVCaptureDeviceFormat *format in [videoDevice formats] ) {
        for ( AVFrameRateRange *range in format.videoSupportedFrameRateRanges ) {
            if ( range.maxFrameRate > bestFrameRateRange.maxFrameRate ) {
                bestFormat = format;
                bestFrameRateRange = range;
            }
        }
    }
    if ( bestFormat) {
        if ( [videoDevice lockForConfiguration:NULL] == YES ) {
            videoDevice.activeFormat = bestFormat;
            videoDevice.activeVideoMinFrameDuration = CMTimeMake(1,30);
            videoDevice.activeVideoMaxFrameDuration = CMTimeMake(1,30);
            [videoDevice unlockForConfiguration];
        }
    }

}
-(void)setupOutput{
    //苹果不支持rgb渲染
    AVCaptureVideoDataOutput *videoOutput = [[AVCaptureVideoDataOutput alloc]  init];
    //帧率 1秒15帧
//    videoOutput.minFrameDuration = CMTimeMake(1, 15);
    
    //指定解码之后的图像格式
    videoOutput.videoSettings = @{(NSString *)kCVPixelBufferPixelFormatTypeKey:@(kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange)};
    
    //设置串行队列 保证数据顺序
    dispatch_queue_t queue = dispatch_queue_create("AGORAVIDEOCAPTURE", DISPATCH_QUEUE_SERIAL);
    [videoOutput setSampleBufferDelegate:self queue:queue];
    //给会话添加输出对象
    if ([_captureSession canAddOutput:videoOutput]) {
        [_captureSession addOutput:videoOutput];
    }
    //获取输入与输出之间的链接
    //设置采集的方向 镜像
    AVCaptureConnection *connection = [videoOutput connectionWithMediaType:AVMediaTypeVideo];
    connection.videoOrientation = AVCaptureVideoOrientationPortrait;
    connection.videoMirrored  = YES;
    

    
    
    
}
//选择前置摄像头
- (AVCaptureDevice *)deviceWithPosition:(AVCaptureDevicePosition)position {
    NSArray *devices = [AVCaptureDevice devices];
    for (AVCaptureDevice *device in devices) {
        if(device.position == AVCaptureDevicePositionFront) {
            return device;
        }
    }
    return nil;
}
//获取帧数据
- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    // captureSession 会话如果没有强引用，这里不会得到执行
    
    // 获取图片帧数据
    if (self.delegate && [self.delegate respondsToSelector:@selector(onCaptureCallback:)]) {
        [self.delegate onCaptureCallback:sampleBuffer];
    }
}


@end
