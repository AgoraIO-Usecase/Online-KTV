//
//  MainViewController.m
//  AgoraShareVideo_ijkplayer
//
//  Created by 湛孝超 on 2018/4/25.
//  Copyright © 2018年 湛孝超. All rights reserved.
//

#import "MainViewController.h"
#import "AgoraAudioFrame.h"
#import <IJKMediaFramework/IJKMediaFramework.h>
#import <AgoraRtcEngineKit/AgoraRtcEngineKit.h>
@interface MainViewController ()<AgoraRtcEngineDelegate>
@property (weak, nonatomic) IBOutlet UIView *videoContainerView;
@property(atomic, retain) id<IJKMediaPlayback> player;
@property(nonatomic,strong) AgoraRtcEngineKit *rtcEngine;
@end

@implementation MainViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    NSError *error;
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayAndRecord error:&error];
    
    self.rtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:@"<#APP_ID#>" delegate:self];
    [self.rtcEngine enableAudio];
    [self.rtcEngine enableVideo];
    [self.rtcEngine setAudioProfile:AgoraAudioProfileMusicHighQuality scenario:AgoraAudioScenarioGameStreaming];
    [self.rtcEngine setChannelProfile:AgoraChannelProfileLiveBroadcasting];
    [self.rtcEngine setClientRole:AgoraClientRoleBroadcaster];
    [self.rtcEngine setVideoResolution:CGSizeMake(768, 432) andFrameRate:25 bitrate:1300];
    [self.rtcEngine setRecordingAudioFrameParametersWithSampleRate:44100 channel:2 mode:AgoraAudioRawFrameOperationModeReadWrite samplesPerCall:882];
    
    [self.rtcEngine muteAllRemoteVideoStreams:true];
    //加入房间
    [self.rtcEngine setExternalVideoSource:true useTexture:false pushMode:true];
    [self.rtcEngine setParameters:@"{\"che.audio.use.remoteio\":true}"];
    [[AgoraAudioFrame shareInstance] registerEngineKit:self.rtcEngine];
    [self.rtcEngine joinChannelByToken:nil channelId:@"hyy" info:nil uid:0 joinSuccess:^(NSString * _Nonnull channel, NSUInteger uid, NSInteger elapsed) {
    }];
    //注册视频通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(ijk_video_CallBack:) name:@"ijk_video_CallBack" object:nil];
    //注册音频回调通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(ijk_Audio_CallBack:) name:@"ijk_Audio_CallBack" object:nil];
}
-(void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    
    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
    //获取视频的地址
    NSString *videoPath  =  [[NSBundle mainBundle] pathForResource:@"123.mp4" ofType:nil];
    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:[NSURL URLWithString:videoPath] withOptions:options];
    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    self.player.view.frame = self.videoContainerView.bounds;
    self.player.scalingMode = IJKMPMovieScalingModeAspectFit;
    self.player.shouldAutoplay = false;
    self.videoContainerView.autoresizesSubviews = YES;
    [self.videoContainerView addSubview:self.player.view];
    [self.player prepareToPlay];

}
//ijk 数据抛出的处理
-(void)ijk_video_CallBack:(NSNotification *)notification
{
    CVImageBufferRef imagebuf = (__bridge CVImageBufferRef)(notification.object);
    AgoraVideoFrame *videoFrame = [[AgoraVideoFrame alloc] init];
    videoFrame.format = 12;
    videoFrame.time = CMTimeMake(CACurrentMediaTime()*1000, 1000);
    videoFrame.textureBuf = imagebuf;
    [self.rtcEngine pushExternalVideoFrame:videoFrame];
}
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


/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
