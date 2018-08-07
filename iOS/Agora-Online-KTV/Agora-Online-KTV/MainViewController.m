//
//  MainViewController.m
//  Agora-Online-KTV
//
//  Created by 湛孝超 on 2018/4/25.
//  Copyright © 2018年 湛孝超. All rights reserved.
//

#import "MainViewController.h"
#import "AgoraAudioFrame.h"
#import <IJKMediaFramework/IJKMediaFramework.h>
#import <AgoraRtcEngineKit/AgoraRtcEngineKit.h>
#import "AgoraVideoCapture.h"
#import "AgoraMixVideo.h"
@interface MainViewController ()<AgoraRtcEngineDelegate>
{
    BOOL isChangeAudio;
}
@property (weak, nonatomic) IBOutlet UIView *videoContainerView;
@property(atomic, retain) id<IJKMediaPlayback> player;
@property(nonatomic,strong) AgoraRtcEngineKit *rtcEngine;
@property(nonatomic,strong) AgoraVideoCapture *capture;
@property(nonatomic,strong) AgoraMixVideo *mixVideo;
@end

@implementation MainViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.mixVideo = [[AgoraMixVideo alloc] init];
    __weak typeof(self) weakSelf = self;
    self.mixVideo.onVideoCallBack = ^(uint8_t *buf, size_t yStride, size_t localWidth, size_t localHeight) {
        
                AgoraVideoFrame *videoFrame = [[AgoraVideoFrame alloc] init];
                videoFrame.format = 1;
                videoFrame.time = CMTimeMake(CACurrentMediaTime()*1000, 1000);
                videoFrame.strideInPixels = (int)yStride;
                 videoFrame.height = (int)localHeight;
                videoFrame.dataBuf = [NSData dataWithBytes:buf length:localWidth * localHeight * 1.5];
                [weakSelf.rtcEngine pushExternalVideoFrame:videoFrame];
//                NSLog(@"%d",ret);
    };
    
    isChangeAudio = false;
    self.rtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:@"aab8b8f5a8cd4469a63042fcfafe7063" delegate:self];
    [self.rtcEngine enableAudio];
    [self.rtcEngine enableVideo];
    [self.rtcEngine setAudioProfile:AgoraAudioProfileMusicHighQuality scenario:AgoraAudioScenarioGameStreaming];
    [self.rtcEngine setChannelProfile:AgoraChannelProfileLiveBroadcasting];
    [self.rtcEngine setClientRole:AgoraClientRoleBroadcaster];
    //设置推送的视频分辨率 帧率 和 码率
    [self.rtcEngine setVideoResolution:CGSizeMake(848, 480) andFrameRate:30 bitrate:1300];
    //mv的音频采样率和SDK的录制的音频的数据采样率保持一致
    [self.rtcEngine setRecordingAudioFrameParametersWithSampleRate:16000 channel:2 mode:AgoraAudioRawFrameOperationModeReadWrite samplesPerCall:320];
    [self.rtcEngine setPlaybackAudioFrameParametersWithSampleRate:16000 channel:2 mode:AgoraAudioRawFrameOperationModeReadWrite samplesPerCall:320];

    //开启耳返
    [self.rtcEngine enableInEarMonitoring:true];
    //加入房间
    [self.rtcEngine setExternalVideoSource:true useTexture:false pushMode:true];
    [self.rtcEngine setParameters:@"{\"che.audio.use.remoteio\":true}"];
    [[AgoraAudioFrame shareInstance] registerEngineKit:self.rtcEngine];
    [self.rtcEngine joinChannelByToken:nil channelId:@"hyy" info:nil uid:0 joinSuccess:^(NSString * _Nonnull channel, NSUInteger uid, NSInteger elapsed) {
    }];
    
    //注册音频回调通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(ijk_Audio_CallBack:) name:@"ijk_Audio_CallBack" object:nil];
}
-(void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
//    
    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
    //开启硬件解码器
    [options setOptionIntValue:1 forKey:@"videotoolbox" ofCategory:kIJKFFOptionCategoryPlayer];
    //获取视频的地址
    NSString *videoPath  =  [[NSBundle mainBundle] pathForResource:@"1080.mp4" ofType:nil];
    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:[NSURL URLWithString:videoPath] withOptions:options];
    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    self.player.view.frame = self.videoContainerView.bounds;
    self.player.scalingMode = IJKMPMovieScalingModeFill;
    self.player.shouldAutoplay = false;
    self.videoContainerView.autoresizesSubviews = YES;
    [self.videoContainerView addSubview:self.player.view];
    [self.player prepareToPlay];

}
//得到音频数据的处理
-(void)ijk_Audio_CallBack:(NSNotification *)notification
{
    NSData *data = notification.object;
    char * p = (char *)[data bytes];
    [[AgoraAudioFrame shareInstance] pushAudioSource:p byteLength:data.length];
    
}
- (IBAction)video_click:(id)sender {
    [self.player play];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}
- (IBAction)video_stop:(id)sender {
    [self.player pause];
}
- (IBAction)ChangeAudioStream:(id)sender {
    isChangeAudio =! isChangeAudio;
//    [self.player setPlaybackVolume:0];
    [self.player changeAudioStream:isChangeAudio];
}
- (IBAction)singleAnchor:(id)sender {
    self.mixVideo.isOpenCapture = true;
    self.mixVideo.captureAndMV = false;
}
- (IBAction)anchorAndMV:(id)sender {
    self.mixVideo.captureAndMV = true;
    self.mixVideo.isOpenCapture = true;
    
}
- (IBAction)mvMode:(id)sender {
    self.mixVideo.captureAndMV = true;
    self.mixVideo.isOpenCapture = false;
}
- (IBAction)volumeChange:(UISlider *)sender {
    
    [self.player setPlaybackVolume:sender.value];
}



@end
