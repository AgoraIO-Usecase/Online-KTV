//
//  KTVKit.h
//  Agora-Online-KTV
//
//  Created by zhanxiaochao on 2018/9/20.
//  Copyright © 2018年 Agora. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AgoraRtcEngineKit/AgoraRtcEngineKit.h>
NS_ASSUME_NONNULL_BEGIN
typedef NS_ENUM(NSInteger, KTVStatusType)
{
    KTVStatusTypePrepared = 1,
    KTVStatusTypeCompleted = 2,
    
};


@protocol KTVKitDelegate <NSObject>

-(void)ktvStatusCallBack:(KTVStatusType)ktvStatusType;

@end

@interface KTVKit : NSObject
+ (instancetype)shareInstance;
@property (nonatomic, assign) id<KTVKitDelegate>delegate;
//创建KTVKit
-(void)createKTVKit:(NSString *)videoPath withView:(UIView *)view rtcEngine:(AgoraRtcEngineKit *)rtcEngine withsampleRate:(int)sampleRate ;
//切歌
-(void)switchMV:(NSString *)videoPath;
//获取视频时长
-(double)getDuation;
//获取当前的视频位置
-(float)getCurrentPosition;
//原唱伴奏切换
-(void)switchAudioTrack:(BOOL)isSwitch;
//调整伴奏音量
-(void)adjustVoiceVolume:(double )volume;
//调整背景音量
-(void)adjustAccompanyVolume:(double)volume;
//视频的播放/暂停
-(void)play;
-(void)pause;
//引擎释放
-(void)resume;
//引擎销毁
-(void)destroyKTVKit;


@end

NS_ASSUME_NONNULL_END
