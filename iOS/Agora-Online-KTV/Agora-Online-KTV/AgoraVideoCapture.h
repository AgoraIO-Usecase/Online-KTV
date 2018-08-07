//
//  AgoraVideoCapture.h
//  Agora-Online-KTV
//
//  Created by zhanxiaochao on 2018/8/1.
//  Copyright © 2018年 湛孝超. All rights reserved.
//

#import <Foundation/Foundation.h>
#import  <AVFoundation/AVFoundation.h>

@protocol AgoraVideoCaptureDelegate<NSObject>
-(void)onCaptureCallback:(CMSampleBufferRef)sampleBuf;
@end
@interface AgoraVideoCapture : NSObject
@property (nonatomic, assign) id<AgoraVideoCaptureDelegate>delegate;
@end
