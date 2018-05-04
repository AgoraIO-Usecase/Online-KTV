//
//  LiveRoomViewController.m
//  AgoraShareVideoAudience
//
//  Created by GongYuhua on 2016/9/12.
//  Copyright © 2016年 Agora. All rights reserved.
//

#import "LiveRoomViewController.h"
#import "VideoSession.h"
#import "VideoViewLayouter.h"
#import "KeyCenter.h"
#import <AgoraRtcEngineKit/IAgoraMediaEngine.h>
#import <AgoraRtcEngineKit/IAgoraRtcEngine.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioToolbox/AudioToolbox.h>


@interface LiveRoomViewController () <AgoraRtcEngineDelegate>

@property (weak, nonatomic) IBOutlet UILabel *roomNameLabel;
@property (weak, nonatomic) IBOutlet UIView *remoteContainerView;
@property (weak, nonatomic) IBOutlet UIButton *broadcastButton;
@property (strong, nonatomic) IBOutletCollection(UIButton) NSArray *sessionButtons;
@property (weak, nonatomic) IBOutlet UIButton *audioMuteButton;
@property (weak, nonatomic) IBOutlet UIButton *enhancerButton;

@property (strong, nonatomic) AgoraRtcEngineKit *rtcEngine;
@property (assign, nonatomic) BOOL isBroadcaster;
@property (assign, nonatomic) BOOL isMuted;
@property (assign, nonatomic) BOOL shouldEnhancer;
@property (strong, nonatomic) NSMutableArray<VideoSession *> *videoSessions;
@property (strong, nonatomic) VideoSession *fullSession;
@property (strong, nonatomic) VideoViewLayouter *viewLayouter;
@property (assign, nonatomic) NSUInteger localUid;
@property (assign, nonatomic) NSUInteger remoteUid;
@property (assign, nonatomic) NSUInteger userCount;
@property (strong, nonatomic) NSURL * mp3Url;
@property (strong, nonatomic) AVAudioPlayer *player;

@end

@implementation LiveRoomViewController
- (BOOL)isBroadcaster {
    return self.clientRole == AgoraClientRoleBroadcaster;
}

- (VideoViewLayouter *)viewLayouter {
    if (!_viewLayouter) {
        _viewLayouter = [[VideoViewLayouter alloc] init];
    }
    return _viewLayouter;
}

- (void)setClientRole:(AgoraClientRole)clientRole {
    _clientRole = clientRole;
    
    if (self.isBroadcaster) {
        self.shouldEnhancer = YES;
    }
    [self updateButtonsVisiablity];
}

- (void)setIsMuted:(BOOL)isMuted {
    _isMuted = isMuted;
    [self.rtcEngine muteLocalAudioStream:isMuted];
    [self.audioMuteButton setImage:[UIImage imageNamed:(isMuted ? @"btn_mute_cancel" : @"btn_mute")] forState:UIControlStateNormal];
}

- (void)setVideoSessions:(NSMutableArray<VideoSession *> *)videoSessions {
    _videoSessions = videoSessions;
    if (self.remoteContainerView) {
        [self updateInterfaceWithAnimation:YES];
    }
}

- (void)setFullSession:(VideoSession *)fullSession {
    _fullSession = fullSession;
    if (self.remoteContainerView) {
        [self updateInterfaceWithAnimation:YES];
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.videoSessions = [[NSMutableArray alloc] init];
    
    self.roomNameLabel.text = self.roomName;
    [self updateButtonsVisiablity];
    self.userCount = 0;
    [self loadAgoraKit];
}

- (IBAction)doSwitchCameraPressed:(UIButton *)sender {
    [self.rtcEngine switchCamera];
}

- (IBAction)doMutePressed:(UIButton *)sender {
    self.isMuted = !self.isMuted;
}

- (IBAction)doBroadcastPressed:(UIButton *)sender {
    if (self.isBroadcaster) {
        self.clientRole = AgoraClientRoleAudience;
        if (self.fullSession.uid == 0) {
            self.fullSession = nil;
        }
    } else {
        self.clientRole = AgoraClientRoleBroadcaster;
    }
    
    [self.rtcEngine setClientRole:self.clientRole];
    
    [self updateInterfaceWithAnimation:YES];
}

- (IBAction)doDoubleTapped:(UITapGestureRecognizer *)sender {
    if (!self.fullSession) {
        VideoSession *tappedSession = [self.viewLayouter responseSessionOfGesture:sender inSessions:self.videoSessions inContainerView:self.remoteContainerView];
        if (tappedSession) {
            self.fullSession = tappedSession;
        }
    } else {
        self.fullSession = nil;
    }
}

- (IBAction)doLeavePressed:(UIButton *)sender {
    [self leaveChannel];
}

- (void)updateButtonsVisiablity {
    [self.broadcastButton setImage:[UIImage imageNamed:self.isBroadcaster ? @"btn_join_cancel" : @"btn_join"] forState:UIControlStateNormal];
    for (UIButton *button in self.sessionButtons) {
        button.hidden = !self.isBroadcaster;
    }
}

- (void)leaveChannel {
    [self setIdleTimerActive:YES];
    
    [self.rtcEngine setupLocalVideo:nil];
    [self.rtcEngine leaveChannel:nil];
    if (self.isBroadcaster) {
        [self.rtcEngine stopPreview];
    }
    
    for (VideoSession *session in self.videoSessions) {
        [session.hostingView removeFromSuperview];
    }
    [self.videoSessions removeAllObjects];
    
    if ([self.delegate respondsToSelector:@selector(liveVCNeedClose:)]) {
        [self.delegate liveVCNeedClose:self];
    }
}

- (void)setIdleTimerActive:(BOOL)active {
    [UIApplication sharedApplication].idleTimerDisabled = !active;
}

- (void)alertString:(NSString *)string {
    if (!string.length) {
        return;
    }
    
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:nil message:string preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"Ok" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
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
    NSArray *displaySessions;
    if (!self.isBroadcaster && self.videoSessions.count) {
        displaySessions = [self.videoSessions subarrayWithRange:NSMakeRange(1, self.videoSessions.count - 1)];
    } else {
        displaySessions = [self.videoSessions copy];
    }
    
    [self.viewLayouter layoutSessions:displaySessions fullSession:self.fullSession inContainer:self.remoteContainerView];
    [self setStreamTypeForSessions:displaySessions fullSession:self.fullSession];
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

- (void)addLocalSession {
    VideoSession *localSession = [VideoSession localSession];
    [self.videoSessions addObject:localSession];
    [self.rtcEngine setupLocalVideo:localSession.canvas];
    [self updateInterfaceWithAnimation:YES];
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

//使用推流功能，appid需要开通相应的权限，否则推流会失败
- (void)setPublishConfig:(NSString *)rtmpURL
{
    AgoraPublisherConfiguration *builder = [[AgoraPublisherConfiguration alloc] init];
    [builder setOwner:YES];
    [builder setLifeCycle:AgoraRtmpStreamLifeCycleBindToOwnner];
    [builder setWidth:360];
    [builder setHeight:640];
    [builder setFramerate:15];
    [builder setBitrate:1200];
    [builder setPublishUrl:rtmpURL];
    [builder setDefaultLayout:3];
    
    [self.rtcEngine configPublisher:builder];
}

-(void)setVideoCompositingLayout:(NSInteger)userCount
{
    [self.rtcEngine clearVideoCompositingLayout];
    
    if (userCount == 1) {
        AgoraRtcVideoCompositingRegion *region = [[AgoraRtcVideoCompositingRegion alloc] init];
        region.uid = self.localUid;
        region.x = 0.0;
        region.y = 0.0;
        region.width = 1.0;
        region.height = 1.0;
        region.zOrder = 0;
        region.alpha = 1.0;
        region.renderMode = AgoraVideoRenderModeFit;
        AgoraRtcVideoCompositingLayout *layout = [[AgoraRtcVideoCompositingLayout alloc] init];
        layout.regions = [NSArray arrayWithObjects:region, nil];
        [self.rtcEngine setVideoCompositingLayout:layout];
        
    }
    else if (userCount == 2)
    {
        AgoraRtcVideoCompositingRegion *region1 = [[AgoraRtcVideoCompositingRegion alloc] init];
        region1.uid = self.localUid;
        region1.x = 0.0;
        region1.y = 0.0;
        region1.width = 1.0;
        region1.height = 0.5;
        region1.zOrder = 0;
        region1.alpha = 1.0;
        region1.renderMode = AgoraVideoRenderModeFit;
        AgoraRtcVideoCompositingRegion *region2 = [[AgoraRtcVideoCompositingRegion alloc] init];
        region2.uid = self.remoteUid;
        region2.x = 0.0;
        region2.y = 0.5;
        region2.width = 1.0;
        region2.height = 0.5;
        region2.zOrder = 0;
        region2.alpha = 1.0;
        region2.renderMode = AgoraVideoRenderModeFit;
        AgoraRtcVideoCompositingLayout *layout = [[AgoraRtcVideoCompositingLayout alloc] init];
        layout.regions = [NSArray arrayWithObjects:region1, region2, nil];
        [self.rtcEngine setVideoCompositingLayout:layout];
    }
}
- (void)loadAgoraKit {

    
    self.rtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:[KeyCenter AppId] delegate:self];
    [self.rtcEngine setChannelProfile:AgoraChannelProfileLiveBroadcasting];
    [self.rtcEngine enableDualStreamMode:true];
    [self.rtcEngine enableAudio];
    [self.rtcEngine enableVideo];
    [self.rtcEngine setVideoProfile:self.videoProfile swapWidthAndHeight:YES];
    [self.rtcEngine setClientRole:self.clientRole];
    [self.rtcEngine setParameters:@"{\"che.audio.stream_type\":\"3\"}"];
    [self.rtcEngine setRecordingAudioFrameParametersWithSampleRate:48000 channel:1 mode:AgoraAudioRawFrameOperationModeReadWrite samplesPerCall:1024];
    [self.rtcEngine setPlaybackAudioFrameParametersWithSampleRate:48000 channel:1 mode:AgoraAudioRawFrameOperationModeReadWrite samplesPerCall:1024];
//    if (self.isBroadcaster) {
//        [self.rtcEngine startPreview];
//    }
//    [self setPublishConfig:@"rtmp://vid-218.push.chinanetcenter.broadcastapp.agora.io/live/123"];
    
    NSLog(@"%@",[AgoraRtcEngineKit getSdkVersion]);
    [self addLocalSession];
    int code = [self.rtcEngine joinChannelByToken:nil channelId:self.roomName info:nil uid:0 joinSuccess:nil];
    if (code == 0) {
        [self setIdleTimerActive:NO];
     int ret =  [self.rtcEngine setEnableSpeakerphone:true];
        NSLog(@"%d",ret);
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self alertString:[NSString stringWithFormat:@"Join channel failed: %d", code]];
        });
    }
    if (self.isBroadcaster) {
        self.shouldEnhancer = YES;
    }
}

-(void)rtcEngine:(AgoraRtcEngineKit *)engine didJoinChannel:(NSString *)channel withUid:(NSUInteger)uid elapsed:(NSInteger)elapsed{
    self.localUid = uid;

    [self setVideoCompositingLayout:1];

}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didJoinedOfUid:(NSUInteger)uid elapsed:(NSInteger)elapsed {
    VideoSession *userSession = [self videoSessionOfUid:uid];
    [self.rtcEngine setupRemoteVideo:userSession.canvas];
    self.remoteUid = uid;
    [self setVideoCompositingLayout:2];
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine firstLocalVideoFrameWithSize:(CGSize)size elapsed:(NSInteger)elapsed {
    if (self.videoSessions.count) {
        [self updateInterfaceWithAnimation:NO];
    }
}
-(void)rtcEngine:(AgoraRtcEngineKit *)engine firstRemoteVideoFrameOfUid:(NSUInteger)uid size:(CGSize)size elapsed:(NSInteger)elapsed{
    
    
    
}
- (void)rtcEngine:(AgoraRtcEngineKit *)engine didOfflineOfUid:(NSUInteger)uid reason:(AgoraUserOfflineReason)reason {
    VideoSession *deleteSession;
    for (VideoSession *session in self.videoSessions) {
        if (session.uid == uid) {
            deleteSession = session;
        }
    }
    
    self.remoteUid = 0;
    [self setVideoCompositingLayout:1];
    
    if (deleteSession) {
        [self.videoSessions removeObject:deleteSession];
        [deleteSession.hostingView removeFromSuperview];
        [self updateInterfaceWithAnimation:YES];
        
        if (deleteSession == self.fullSession) {
            self.fullSession = nil;
        }
    }
}
@end

