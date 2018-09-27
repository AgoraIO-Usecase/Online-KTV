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
#import "KTVKit.h"
@interface AgoraVideoViewController ()<AgoraRtcEngineDelegate,KTVKitDelegate>
{
    BOOL isChangeAudio;
    NSInteger uid;
}
@property (weak, nonatomic) IBOutlet UIView *videoContainerView;
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
    //默认播放音轨为2的伴奏  如果遇到伴奏音轨在1的情况 设置这个值为false
    isChangeAudio = true;
    //设置
    self.videoSessions = [[NSMutableArray alloc] init];
    [self hiddenButtons:!self.isBroadcaster];
    NSLog(@"SDKVersion -- %@",AgoraRtcEngineKit.getSdkVersion);
    self.rtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:@"<#APP_ID#>" delegate:self];
    [self.rtcEngine enableAudio];
    [self.rtcEngine enableVideo];
    [self.rtcEngine enableDualStreamMode:false];
    [self.rtcEngine setAudioProfile:AgoraAudioProfileMusicHighQuality scenario:AgoraAudioScenarioGameStreaming];
    [self.rtcEngine setChannelProfile:AgoraChannelProfileLiveBroadcasting];
    [self.rtcEngine setVideoProfile:AgoraVideoProfilePortrait360P swapWidthAndHeight:false];
    [self.rtcEngine setClientRole:self.clientRole];
    //KTVKit 模块的初始化 设置视频地址 设置需要绑定的渲染的View 绑定RTCEngine 设置MV的音频采样率
    [[KTVKit shareInstance] createKTVKitWithView:self.videoContainerView rtcEngine:self.rtcEngine withsampleRate:44100];
    [KTVKit shareInstance].delegate = self;
    //判断是否是主播 主播预加载好MV
    if ([self isBroadcaster]) {
        //加载视频地址

        [[KTVKit shareInstance] loadMV:@"http://compress.mv.letusmix.com/1c4dbe546537a9459d7b2c208d513303.mp4"];
        
    }
//    [[AgoraAudioFrame shareInstance] registerEngineKit:_rtcEngine];
    [self.rtcEngine joinChannelByToken:nil channelId:self.roomName info:nil uid:0 joinSuccess:nil];
    
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

- (IBAction)video_click:(id)sender {
    [[KTVKit shareInstance] play];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}
- (IBAction)video_stop:(id)sender {
    [[KTVKit shareInstance] pause];
}
- (IBAction)ChangeAudioStream:(id)sender {
    isChangeAudio =! isChangeAudio;
    [[KTVKit shareInstance] switchAudioTrack:isChangeAudio];
}

//调节播放/伴奏音量大小
- (IBAction)accompanyVolumeChange:(UISlider *)sender {
    
    //改变伴奏大小的属性
    [[KTVKit shareInstance] adjustAccompanyVolume:sender.value];
    
}
- (IBAction)voiceNumChange:(UISlider *)sender {
    //改成人声大小的属性
    [[KTVKit shareInstance] adjustVoiceVolume:sender.value];
}
- (IBAction)doMutePressed:(id)sender {
    self.isMuted = !self.isMuted;
}
//上下麦切换
- (IBAction)doBroadcasterPressed:(id)sender {
    
    if (self.isBroadcaster) {
        self.clientRole = AgoraClientRoleAudience;
        //下麦KTV模块重启
        [[KTVKit shareInstance] resume];
        
        if (self.fullSession.uid == 0) {
            self.fullSession = nil;
        }
    } else {
        
        self.clientRole = AgoraClientRoleBroadcaster;
        //上麦时KTV模块加载歌曲
        //默认播放音轨为2的伴奏  如果遇到伴奏音轨在1的情况 设置这个值为false
        isChangeAudio = true;
        [[KTVKit shareInstance] loadMV:@"http://compress.mv.letusmix.com/1c4dbe546537a9459d7b2c208d513303.mp4"];
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
    NSLog(@"uid === %lu",(unsigned long)uid);
    
//    [self.rtcEngine setParameters(@"{\"che.audio.playout.uid.volume\":{\"uid\":hostUid,\"volume\":30}}")];
    
    
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
    //默认播放音轨为2的伴奏  如果遇到伴奏音轨在1的情况 设置这个值为false
    isChangeAudio = true;
    //切歌执行的操作
    [[KTVKit shareInstance] loadMV:@"http://compress.mv.letusmix.com/914184d11605138c7de8c28f2905c63a.mp4"];
}
//离开房间执行
- (IBAction)leaveChannel:(id)sender {
    
    //KTV模块销毁
    [[KTVKit shareInstance] destroyKTVKit];
    
    [self.rtcEngine leaveChannel:nil];
    [AgoraRtcEngineKit destroy];
    [self.navigationController  popViewControllerAnimated:true];
    
}
-(void)ktvStatusCallBack:(KTVStatusType)ktvStatusType
{
    switch (ktvStatusType) {
        case KTVStatusTypePrepared:
        {
            UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"KTVStatus" message:@"加载完成" delegate:nil cancelButtonTitle:@"确定" otherButtonTitles:nil];
            [alertView show];
            break;
        }
        case KTVStatusTypeCompleted:{
            UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"KTVStatus" message:@"播放完成" delegate:nil cancelButtonTitle:@"确定" otherButtonTitles:nil];
            [alertView show];
            break;
        }
            
        default:{
            break;
        }
    }
}


@end

