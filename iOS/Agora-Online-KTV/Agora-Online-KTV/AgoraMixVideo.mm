//
//  AgoraMixVideo.m
//  Agora-Online-KTV
//
//  Created by zhanxiaochao on 2018/8/1.
//  Copyright © 2018年 湛孝超. All rights reserved.
//
extern "C" {
#import "libyuv-iOS/include/libyuv.h"
}
#import "AgoraMixVideo.h"
#import "AgoraVideoBufferConverter.h"
#import "AgoraVideoCapture.h"

//摄像头采集的分辨率
#define CAPTUREWIDTH 480
#define CAPTUREHEIGHT 640

//缩放之后的图像大小
#define SCALEWIDTH  240
#define SCALEHEIGHT 240

//居右偏移
#define offset_right 10
//居上偏移
#define offset_top 0

static NSObject *threadLockCapture = [[NSObject alloc] init];
static NSObject *threadLockPlay = [[NSObject alloc] init];


@interface MixVideo:NSObject
@property(atomic,assign) uint8_t * videoCaptureBuf;

@end
@implementation MixVideo

@end
@interface AgoraMixVideo()<AgoraVideoCaptureDelegate>
{
    size_t rotate_strideY;
    size_t rotateWidth;
    size_t rotateHeight;
    NSTimer *timer;
    uint8_t tmp[CAPTUREWIDTH * CAPTUREHEIGHT * 3 / 2];
}
@property(nonatomic, strong) AgoraVideoCapture *capture;
@property(nonatomic,assign) size_t r_size;
@end
@implementation AgoraMixVideo
-(instancetype)init
{
    if (self = [super init]) {
        self.capture = [[AgoraVideoCapture alloc] init];
        self.capture.delegate = self;
        [self initSetup];
    }
    return self;
}

//初始化设置
-(void)initSetup{
    
    //注册视频通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(ijk_video_CallBack:) name:@"ijk_video_CallBack" object:nil];
    
}
//ijk 数据抛出的处理
-(void)ijk_video_CallBack:(NSNotification *)notification
{
    if (self.isOpenCapture && !self.captureAndMV) {
        return;
    }
    CVPixelBufferRef buffer = (__bridge CVPixelBufferRef)(notification.object);
    CVPixelBufferRef texBuf = buffer;
    if(!texBuf) {
        return ;
    }
    const int kFlags = 0;
    if (CVPixelBufferLockBaseAddress(texBuf, kFlags) != kCVReturnSuccess) {
        //        printf("failed to lock base address\n");
        return ;
    }
    OSType fmt = CVPixelBufferGetPixelFormatType(texBuf);
    if(fmt == (('4' << 24) | ('2' << 16) | ('0' << 8) | 'f') ||
       fmt == (('4' << 24) | ('2' << 16) | ('0' << 8) | 'v')) { // kCVPixelFormatType_420YpCbCr8BiPlanarFullRange or kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
        const int kYPlaneIndex = 0;
        const int kUVPlaneIndex = 1;
        uint8_t* ybuf          = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(texBuf, kYPlaneIndex);
        int yPlaneBytesPerRow  = (int)CVPixelBufferGetBytesPerRowOfPlane(texBuf, kYPlaneIndex);
        int yPlaneWidth        = (int)CVPixelBufferGetWidthOfPlane      (texBuf, kYPlaneIndex);
        int yPlaneHeight       = (int)CVPixelBufferGetHeightOfPlane     (texBuf, kYPlaneIndex);
        uint8_t* cbuf          = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(texBuf, kUVPlaneIndex);
        int uvPlaneBytesPerRow = (int)CVPixelBufferGetBytesPerRowOfPlane(texBuf, kUVPlaneIndex);
        size_t left = 0;
        size_t right = 0;
        size_t top = 0;
        size_t bottom = 0;
        CVPixelBufferGetExtendedPixels(texBuf, &left, &right, &top, &bottom);
        right += yPlaneBytesPerRow - yPlaneWidth;
        unsigned char *nv12buf = nullptr;
        if(yPlaneBytesPerRow * yPlaneHeight == (int)(cbuf - ybuf) &&
           yPlaneBytesPerRow == uvPlaneBytesPerRow) {
            
            nv12buf = ybuf;
        }
        //将nv12的转换成yuv420p格式
        int src_w = yPlaneWidth;
        int src_h = yPlaneHeight;
        int area = yPlaneBytesPerRow * src_h;
        uint8_t *dstY = (uint8_t *)malloc(src_w * src_h);
        uint8_t *dstUV = (uint8_t *)malloc(src_w * src_h / 2);
        
        
        //yStride 和 width 不一致的情况下 需要拷贝数据
        for (int i = 0; i < yPlaneHeight; i++) {
            memcpy(dstY + yPlaneWidth * i, ybuf + yPlaneBytesPerRow * i, src_w);
        }
        for (int i = 0; i < yPlaneHeight / 2; i++) {
            memcpy(dstUV + yPlaneWidth * i, cbuf + uvPlaneBytesPerRow * i, src_w);
        }
        
        uint8_t *i420buf = (uint8_t *)malloc(src_h *src_w * 3 / 2);
        libyuv::NV12ToI420(dstY, yPlaneWidth, dstUV, yPlaneWidth, i420buf, yPlaneWidth, i420buf+yPlaneWidth*yPlaneHeight, yPlaneWidth>>1, i420buf+5*yPlaneWidth*yPlaneHeight/4, yPlaneWidth>>1, yPlaneWidth, yPlaneHeight);
        
        if (!self.isOpenCapture) {
            if (self.onVideoCallBack) {
                self.onVideoCallBack(i420buf , yPlaneWidth, yPlaneWidth, yPlaneHeight);
            }
            free(dstUV);
            free(dstY);
            free(i420buf);
            CVPixelBufferUnlockBaseAddress(texBuf, kFlags);
            return;
        }
        
        const int dst_w = SCALEWIDTH;
        const int dst_h = SCALEHEIGHT;
        
        const int d_yStride = SCALEWIDTH;
        const int d_uStride = SCALEWIDTH / 2;
        const int d_vStride = SCALEWIDTH / 2;
        //创建头像buf将数据传入buf中
        
        uint8_t *d_ybuf = (uint8_t *)malloc(src_w * src_h);
        uint8_t *d_ubuf = (uint8_t *)malloc(src_w * src_h /4);
        uint8_t *d_vbuf = (uint8_t *)malloc(src_w * src_h /4);
        
        
        //视频画面的放缩
        libyuv::I420Scale(tmp, (int)rotateWidth, tmp + rotateWidth * rotateHeight, (int)rotateWidth >> 1, tmp + rotateWidth * rotateHeight+ rotateWidth * rotateHeight/4 , (int)rotateWidth >> 1, (int)rotateWidth, (int)rotateHeight, d_ybuf, d_yStride, d_ubuf, d_uStride, d_vbuf, d_vStride, dst_w, dst_h, libyuv::kFilterNone);
        int y_size = dst_w  * dst_h;
        uint8_t * pBuffer = (uint8_t *)d_ybuf;
        memcpy(pBuffer + y_size, d_ubuf, y_size/4);
        memcpy(pBuffer + y_size + y_size/4, d_vbuf, y_size/4);
        
        //合图的方法处理
        int off_x = yPlaneWidth - offset_right - SCALEWIDTH;
        int off_y = offset_top;
        int n0ff = 0;
        for (int i = 0; i < dst_h; i++) {
            n0ff = yPlaneWidth * (off_y + i) + off_x;
            memcpy(i420buf + n0ff, d_ybuf + dst_w * i, dst_w);
        }
        for (int j = 0; j < dst_h/2; j++) {
            n0ff = (yPlaneWidth/2) * (off_y/2 + j) + off_x/2;
            memcpy(i420buf+yPlaneWidth*yPlaneHeight + n0ff, d_ubuf + dst_w/2 * j, dst_w/2);
            memcpy(i420buf+5*yPlaneWidth*yPlaneHeight/4 + n0ff, d_vbuf + dst_w/2 * j, dst_w/2);
        }
        //
        if (self.onVideoCallBack) {
            self.onVideoCallBack(i420buf , yPlaneWidth, yPlaneWidth, yPlaneHeight);
        }
        free(d_vbuf);
        free(d_ybuf);
        free(d_ubuf);
        d_ybuf = NULL;
        d_vbuf = NULL;
        d_vbuf = NULL;
        free(dstUV);
        free(dstY);
        free(i420buf);
    }
    CVPixelBufferUnlockBaseAddress(texBuf, kFlags);
    
    
    
}
-(void)onCaptureCallback:(CMSampleBufferRef)sampleBuf{
    @synchronized(threadLockCapture) {
        const int kFlags = 0;
        CVPixelBufferRef videoPiexlBuf = CMSampleBufferGetImageBuffer(sampleBuf);
        
        //    //摄像头的nv12的处理
        
        if (CVPixelBufferLockBaseAddress(videoPiexlBuf, kFlags) != kCVReturnSuccess) {
            //        printf("failed to lock base address\n");
            return ;
        }
        
        Boolean isRotatePlanar = CVPixelBufferIsPlanar(videoPiexlBuf);
        rotateWidth = isRotatePlanar ? CVPixelBufferGetWidthOfPlane(videoPiexlBuf, 0) : CVPixelBufferGetWidth(videoPiexlBuf);
        rotateHeight = isRotatePlanar ? CVPixelBufferGetHeightOfPlane(videoPiexlBuf, 0) : CVPixelBufferGetHeight(videoPiexlBuf);
        
        rotate_strideY = CVPixelBufferGetBytesPerRowOfPlane(videoPiexlBuf, 0);
        size_t rotate_strideUV = CVPixelBufferGetBytesPerRowOfPlane(videoPiexlBuf, 1);
        
        uint8_t* rotateY = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(videoPiexlBuf, 0);
        uint8_t* rotateUV = (uint8_t*)CVPixelBufferGetBaseAddressOfPlane(videoPiexlBuf, 1);
        
        
        int rotateBufferSize  = rotateWidth * rotateHeight * 3 /2 ;
        int r_size = rotateWidth * rotateHeight;
        
        _r_size = r_size;
        //        uint8_t *rBuffer = (uint8 *)malloc(rotateBufferSize);
        uint8_t rBuffer[rotateBufferSize];
        uint8_t *rY = (uint8 *)rBuffer;
        uint8_t *rU = rY + r_size;
        uint8_t *rV = rU + r_size / 4;
        libyuv::NV12ToI420(rotateY, rotate_strideY, rotateUV, rotate_strideUV, rY, rotate_strideY, rU, rotate_strideY>>1, rV, rotate_strideY>>1, rotateWidth, rotateHeight);
        
        if (self.isOpenCapture && !self.captureAndMV) {
            self.onVideoCallBack(rY, rotate_strideY, rotateWidth, rotateHeight);
        }
        memcpy(tmp, rBuffer, r_size);
        memcpy(tmp + r_size, rBuffer + r_size, r_size / 4);
        memcpy(tmp + r_size +r_size/4, rBuffer + r_size + r_size/4, r_size / 4);
        
        CVPixelBufferUnlockBaseAddress(videoPiexlBuf, kFlags);
        
    }
}
@end


