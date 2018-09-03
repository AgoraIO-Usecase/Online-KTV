//
//  MainViewController.m
//  Agora-Online-KTV
//
//  Created by zhanxiaochao on 2018/4/25.
//  Copyright © 2018年 agora. All rights reserved.
//

#import "AgoraVideoViewController.h"
#import "AgoraAudioFrame.h"
#import <IJKMediaFramework/IJKMediaFramework.h>
#import "VideoSession.h"
#import "VideoViewLayouter.h"

@interface AgoraVideoViewController ()<AgoraRtcEngineDelegate>
{
    BOOL isChangeAudio;
}
@property (weak, nonatomic) IBOutlet UIView *videoContainerView;
@property(nonatomic, strong) id<IJKMediaPlayback> player;
@property(nonatomic,strong) AgoraRtcEngineKit *rtcEngine;
@property (assign, nonatomic) BOOL isBroadcaster;
@property (assign, nonatomic) BOOL isMuted;
@property (strong, nonatomic) NSMutableArray<VideoSession *> *videoSessions;
@property (strong, nonatomic) VideoSession *fullSession;
@property (strong, nonatomic) VideoViewLayouter *viewLayouter;
@property (weak, nonatomic) IBOutlet UIButton *changeMVButton;

@property (weak, nonatomic) IBOutlet UIButton *startButton;
@property (weak, nonatomic) IBOutlet UIButton *stopButton;
@property (weak, nonatomic) IBOutlet UIButton *ChangeAudioButton;
@property (weak, nonatomic) IBOutlet UISlider *playBackVolumeButton;
@property (weak, nonatomic) IBOutlet UIButton *BroadCasterButton;
@property (weak, nonatomic) IBOutlet UIButton *MuteAudioButton;
@property (weak, nonatomic) IBOutlet UISlider *songSlider;
@property (weak, nonatomic) IBOutlet UISlider *voiceSlider;
@property (weak, nonatomic) IBOutlet UILabel *voiceLabel;
@property (weak, nonatomic) IBOutlet UILabel *sonLabel;


@end

@implementation AgoraVideoViewController
- (BOOL)isBroadcaster {
    return self.clientRole == AgoraClientRoleBroadcaster;
}
-(void)hiddenButtons:(BOOL)isHidden{
    self.startButton.hidden = isHidden;
    self.stopButton.hidden = isHidden;
    self.ChangeAudioButton.hidden = isHidden;
    self.playBackVolumeButton.hidden = isHidden;
    self.changeMVButton.hidden = isHidden;
    [self.BroadCasterButton setImage:[UIImage imageNamed:self.isBroadcaster ? @"btn_join_cancel" : @"btn_join"] forState:UIControlStateNormal];
    self.songSlider.hidden = isHidden;
    self.voiceSlider.hidden = isHidden;
    self.voiceLabel.hidden  = isHidden;
    self.sonLabel.hidden = isHidden;
    [self.view setNeedsLayout];
}
- (void)viewDidLoad {
    [super viewDidLoad];
    [AgoraAudioFrame shareInstance].sampleRate = 48000;
    self.videoSessions = [[NSMutableArray alloc] init];
    //判断是否是主播
    if (self.isBroadcaster) {
        [self initIjkPlayer:@"http://compress.mv.letusmix.com/914184d11605138c7de8c28f2905c63a.mp4"];
    }else{
        [AgoraAudioFrame shareInstance].isAudience = 1;
    }
    [self hiddenButtons:!self.isBroadcaster];
    isChangeAudio = true;
    
    self.rtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:@"<#APP_ID#>" delegate:self];
    [self.rtcEngine enableAudio];
    [self.rtcEngine enableVideo];
    [self.rtcEngine enableDualStreamMode:true];
    [self.rtcEngine setAudioProfile:AgoraAudioProfileMusicHighQuality scenario:AgoraAudioScenarioGameStreaming];
    [self.rtcEngine setChannelProfile:AgoraChannelProfileLiveBroadcasting];
    [self.rtcEngine setClientRole:self.clientRole];
    //开启耳返
    [self.rtcEngine enableInEarMonitoring:true];
    //    [self.rtcEngine setParameters:@"{\"che.audio.keep.audiosession\":true}"];
    //设置推送的视频分辨率 帧率 和 码率
    [self.rtcEngine setVideoResolution:CGSizeMake(640, 360) andFrameRate:30 bitrate:1300];
    //mv的音频采样率和SDK的录制的音频的数据采样率保持一致
    [self.rtcEngine setRecordingAudioFrameParametersWithSampleRate:48000 channel:2 mode:AgoraAudioRawFrameOperationModeReadWrite samplesPerCall:960];
    [self.rtcEngine setPlaybackAudioFrameParametersWithSampleRate:48000 channel:2 mode:AgoraAudioRawFrameOperationModeReadWrite samplesPerCall:960];
    
    //加入房间
    [self.rtcEngine setExternalVideoSource:true useTexture:false pushMode:true];
    [self.rtcEngine setParameters:@"{\"che.audio.use.remoteio\":true}"];
    [self.rtcEngine setParameters:@"{\"che.audio.keep.audiosession\":true}"];
    //注册引擎回调
    [[AgoraAudioFrame shareInstance] registerEngineKit:self.rtcEngine];
    [self.rtcEngine joinChannelByToken:nil channelId:self.roomName info:nil uid:0 joinSuccess:nil];
    //注册音频回调通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(ijk_Audio_CallBack:) name:@"ijk_Audio_CallBack" object:nil];
    //注册视频回调通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(ijk_video_CallBack:) name:@"ijk_video_CallBack" object:nil];
    //注册音频回调通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(PlaybackDidFinish:) name:IJKMPMoviePlayerPlaybackDidFinishNotification object:nil];
    
    
}
-(void)ijk_video_CallBack:(NSNotification *)notification{
    
    CVPixelBufferRef buffer = (__bridge CVPixelBufferRef)(notification.object);
    AgoraVideoFrame *videoFrame = [[AgoraVideoFrame alloc] init];
    videoFrame.format = 12;
    videoFrame.time = CMTimeMake(CACurrentMediaTime()*1000, 1000);
    videoFrame.textureBuf = buffer;
    [self.rtcEngine pushExternalVideoFrame:videoFrame];
    
}
//得到音频数据的处理
-(void)ijk_Audio_CallBack:(NSNotification *)notification
{
    
    NSData *data = notification.object;
    char * p = (char *)[data bytes];
    [[AgoraAudioFrame shareInstance] pushAudioSource:p byteLength:data.length];
    
}
//播放完成回调
-(void)PlaybackDidFinish:(NSNotification *)not{
    NSLog(@"%@",not.userInfo);
}
-(void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    
}
-(void)setIsMuted:(BOOL)isMuted{
    _isMuted = isMuted;
    [self.rtcEngine muteLocalAudioStream:isMuted];
    [self.MuteAudioButton setImage:[UIImage imageNamed:(isMuted ? @"btn_mute_cancel" : @"btn_mute")] forState:UIControlStateNormal];
}

//初始化ijk
-(void)initIjkPlayer:(NSString *)url{
    if (self.player) {
        [[AgoraAudioFrame shareInstance] destroyAudioBuf];
        [self.player pause];
        //注意一下切歌的逻辑  切歌时先暂停 执行一个过场动画3~5s  再切歌 这样最好 
        
        [self.player stop];
        [self.player shutdown];
        [self.player.view removeFromSuperview];
        self.player = NULL;
        
    }
    
    //设置伴奏音量默认大小
    [AgoraAudioFrame shareInstance].songNum = 0.5;
    //设置人声音量默认大小
    [AgoraAudioFrame shareInstance].voiceNum = 0.5;
    self.songSlider.value = 0.5;
    self.voiceSlider.value = 0.5;
    
    
    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
    //开启硬件解码器
    [options setOptionIntValue:1 forKey:@"videotoolbox" ofCategory:kIJKFFOptionCategoryPlayer];
    //获取视频的地址
    //    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:[NSURL URLWithString:@"http://compress.mv.letusmix.com/914184d11605138c7de8c28f2905c63a.mp4"] withOptions:options];
    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:[NSURL URLWithString:url] withOptions:options];
    
    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    self.player.view.frame = self.videoContainerView.bounds;
    self.player.scalingMode = IJKMPMovieScalingModeFill;
    self.player.shouldAutoplay = false;
    self.videoContainerView.autoresizesSubviews = YES;
    [self.videoContainerView addSubview:self.player.view];
    [self.player prepareToPlay];
    
    
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
    [self.player changeAudioStream:isChangeAudio];
}

//调节播放/伴奏音量大小
- (IBAction)volumeChange:(UISlider *)sender {
    [AgoraAudioFrame shareInstance].songNum = sender.value;
    [self.player setPlaybackVolume:sender.value];
    
}
- (IBAction)voiceNumChange:(UISlider *)sender {
    
    [AgoraAudioFrame shareInstance].voiceNum = sender.value;
    
}
- (IBAction)doMutePressed:(id)sender {
    self.isMuted = !self.isMuted;
}
//上下麦切换
- (IBAction)doBroadcasterPressed:(id)sender {
    
    if (self.isBroadcaster) {
        self.clientRole = AgoraClientRoleAudience;
        if (self.player) {
            [self.player pause];
            [self.player stop];
            [self.player shutdown];
            [self.player.view removeFromSuperview];
            self.player = NULL;
        }
        if (self.fullSession.uid == 0) {
            self.fullSession = nil;
        }
    } else {
        self.clientRole = AgoraClientRoleBroadcaster;
        [self initIjkPlayer:@"http://compress.mv.letusmix.com/914184d11605138c7de8c28f2905c63a.mp4"];
    }
    [self.rtcEngine setClientRole:self.clientRole];
    [self hiddenButtons:!self.isBroadcaster];
}




- (VideoViewLayouter *)viewLayouter {
    if (!_viewLayouter) {
        _viewLayouter = [[VideoViewLayouter alloc] init];
    }
    return _viewLayouter;
}

- (void)setVideoSessions:(NSMutableArray<VideoSession *> *)videoSessions {
    _videoSessions = videoSessions;
    if (self.videoContainerView) {
        [self updateInterfaceWithAnimation:YES];
    }
}

- (void)setFullSession:(VideoSession *)fullSession {
    _fullSession = fullSession;
    if (self.videoContainerView) {
        [self updateInterfaceWithAnimation:YES];
    }
}
- (VideoSession *)fetchSessionOfUid:(NSUInteger)uid {
    for (VideoSession *session in self.videoSessions) {
        if (session.uid == uid) {
            return session;
        }
    }
    return nil;
}


- (VideoSession *)videoSessionOfUid:(NSUInteger)uid {
    VideoSession *fetchedSession = [self fetchSessionOfUid:uid];
    if (fetchedSession) {
        return fetchedSession;
    } else {
        VideoSession *newSession = [[VideoSession alloc] initWithUid:uid];
        [self.videoSessions addObject:newSession];
        [self updateInterfaceWithAnimation:YES];
        return newSession;
    }
}

- (void)updateInterfaceWithAnimation:(BOOL)animation {
    if (animation) {
        [UIView animateWithDuration:0.3 animations:^{
            [self updateInterface];
            [self.view layoutIfNeeded];
        }];
    } else {
        [self updateInterface];
    }
}

- (void)updateInterface {
    
    [self.viewLayouter layoutSessions:self.videoSessions fullSession:self.fullSession inContainer:self.videoContainerView];
    [self setStreamTypeForSessions:self.videoSessions fullSession:self.fullSession];
}

- (void)setStreamTypeForSessions:(NSArray<VideoSession *> *)sessions fullSession:(VideoSession *)fullSession {
    if (fullSession) {
        for (VideoSession *session in sessions) {
            [self.rtcEngine setRemoteVideoStream:session.uid type:(session == self.fullSession ? AgoraVideoStreamTypeHigh : AgoraVideoStreamTypeLow)];
        }
    } else {
        for (VideoSession *session in sessions) {
            [self.rtcEngine setRemoteVideoStream:session.uid type:AgoraVideoStreamTypeHigh];
        }
    }
}
- (void)rtcEngine:(AgoraRtcEngineKit *)engine didJoinedOfUid:(NSUInteger)uid elapsed:(NSInteger)elapsed
{
    NSLog(@"%lu",(unsigned long)uid);
}
-(void)rtcEngine:(AgoraRtcEngineKit *)engine firstRemoteVideoDecodedOfUid:(NSUInteger)uid size:(CGSize)size elapsed:(NSInteger)elapsed
{
    
    VideoSession *userSession = [self videoSessionOfUid:uid];
    [self.rtcEngine setupRemoteVideo:userSession.canvas];
    
    
}
- (void)rtcEngine:(AgoraRtcEngineKit *)engine didOfflineOfUid:(NSUInteger)uid reason:(AgoraUserOfflineReason)reason {
    VideoSession *deleteSession;
    for (VideoSession *session in self.videoSessions) {
        if (session.uid == uid) {
            deleteSession = session;
        }
    }
    if (deleteSession) {
        [self.videoSessions removeObject:deleteSession];
        [deleteSession.hostingView removeFromSuperview];
        [self updateInterfaceWithAnimation:YES];
        
        if (deleteSession == self.fullSession) {
            self.fullSession = nil;
        }
    }
}
//切歌
- (IBAction)changeMV:(id)sender {
    if (self.player == NULL) {
        return;
    }
    [self initIjkPlayer:@"http://compress.mv.letusmix.com/914184d11605138c7de8c28f2905c63a.mp4"];
}
//离开房间执行
- (IBAction)leaveChannel:(id)sender {
    if (self.player) {
        [[AgoraAudioFrame shareInstance] destroyAudioBuf];
        [self.player pause];
        [self.player stop];
        [self.player shutdown];
        [self.player.view removeFromSuperview];
        self.player = NULL;
        
    }
    [self.rtcEngine leaveChannel:nil];
    [AgoraRtcEngineKit destroy];
    [self.navigationController  popViewControllerAnimated:true];
    
}

@end

