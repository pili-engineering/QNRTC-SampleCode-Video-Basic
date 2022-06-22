//
//  RoomViewController.m
//  QNRTCSampleCodeOne
//
//  Created by 冯文秀 on 2019/1/28.
//  Copyright © 2019 Hera. All rights reserved.
//

#import "RoomViewController.h"
#import <QNRTCKit/QNRTCKit.h>

#warning 请到 Podifle 下，重新执行 pod install，确认 Pods/QNRTCKit-iOS/Pod/iphoneos 文件夹下，存在 FFmpeg.framework、QNRTCKit.framework 文件后可运行。

@interface RoomViewController ()
<
QNRTCClientDelegate,
QNLocalAudioTrackDelegate,
QNLocalVideoTrackDelegate,
QNRemoteVideoTrackDelegate,
QNRemoteAudioTrackDelegate
>
@property (nonatomic, assign) CGFloat screenWidth;
@property (nonatomic, assign) CGFloat screenHeight;

@property (nonatomic, strong) UIButton *videoButton;
@property (nonatomic, strong) UIButton *microphoneButton;
@property (nonatomic, strong) UIButton *cameraButton;

@property (nonatomic, strong) NSDictionary *settingsDic;

@property (nonatomic, assign) CGSize videoEncodeSize;
@property (nonatomic, assign) NSInteger kBitrate;

@property (nonatomic, strong) QNRTCClient *rtcClient;
@property (nonatomic, strong) QNMicrophoneAudioTrack *audioTrack;
@property (nonatomic, strong) QNCameraVideoTrack *cameraTrack;

@property (nonatomic, strong) QNVideoGLView *remoteView;

@property (nonatomic, strong) QNVideoGLView *preview;
@end

@implementation RoomViewController

- (UIStatusBarStyle)preferredStatusBarStyle {
    return UIStatusBarStyleLightContent;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.view.backgroundColor = [UIColor colorWithRed:30/255.0 green:30/255.0 blue:30/255.0 alpha:1.0];
        
    self.screenWidth = [UIScreen mainScreen].bounds.size.width;
    self.screenHeight = [UIScreen mainScreen].bounds.size.height;
    
    self.preview = [[QNVideoGLView alloc] init];
    self.preview.frame = CGRectMake(0, 0, self.screenWidth, self.screenHeight);
    [self.view addSubview:self.preview];
    
    [QNRTC enableFileLogging];
    
    // 获取 QNRTCKit 的分辨率、帧率、码率的配置
    self.settingsDic = [self settingsArrayAtIndex:1];
    
    // QNRTCKit 的分辨率
    self.videoEncodeSize = CGSizeFromString(_settingsDic[@"VideoSize"]);
    // QNRTCKit 的码率
    self.kBitrate = [_settingsDic[@"kBitrate"] integerValue];
    
    
    // 配置 QNRTCKit
    [self configureRTC];
    
    // 布局视图
    [self layoutView];
}

#pragma mark - settings

- (NSDictionary *)settingsArrayAtIndex:(NSInteger)index {
    NSArray *settingsArray = @[@{@"VideoSize":NSStringFromCGSize(CGSizeMake(288, 352)), @"FrameRate":@15, @"kBitrate":@(300)},
                        @{@"VideoSize":NSStringFromCGSize(CGSizeMake(480, 640)), @"FrameRate":@15, @"kBitrate":@(400) },
                        @{@"VideoSize":NSStringFromCGSize(CGSizeMake(544, 960)), @"FrameRate":@15, @"kBitrate":@(700)},
                        @{@"VideoSize":NSStringFromCGSize(CGSizeMake(720, 1280)), @"FrameRate":@20, @"kBitrate":@(1000)}];
    return settingsArray[index];
}

#pragma mark - QNRTCKit 核心类

- (void)configureRTC {
    // QNRTC 初始化
    [QNRTC initRTC:[QNRTCConfiguration defaultConfiguration]];

    // QNRTCClient 初始化
    self.rtcClient = [QNRTC createRTCClient];
    self.rtcClient.delegate = self;
    
    // 视频
    QNVideoEncoderConfig *videoConfig = [[QNVideoEncoderConfig alloc] initWithBitrate:self.kBitrate videoEncodeSize:self.videoEncodeSize];
    QNCameraVideoTrackConfig * cameraConfig = [[QNCameraVideoTrackConfig alloc] initWithSourceTag:@"camera" config:videoConfig];
    self.cameraTrack = [QNRTC createCameraVideoTrackWithConfig:cameraConfig];
    // 设置本地预览视图
    [self.cameraTrack play:self.preview];
    
    // 设置采集视频的帧率
    self.cameraTrack.videoFrameRate = [self.settingsDic[@"FrameRate"] integerValue];
    self.cameraTrack.delegate = self;
    
    // 加入房间
    [self.rtcClient join:self.token];
}

#pragma mark - QNRTCClientDelegate 代理回调

/*
 房间内状态变化的回调
 */
- (void)RTCClient:(QNRTCClient *)client didConnectionStateChanged:(QNConnectionState)state disconnectedInfo:(QNConnectionDisconnectedInfo *)info {
    NSDictionary *connectionStateDictionary =  @{@(QNConnectionStateDisconnected) : @"Disconnected",
                                           @(QNConnectionStateConnecting) : @"Connecting",
                                           @(QNConnectionStateConnected): @"Connected",
                                           @(QNConnectionStateReconnecting) : @"Reconnecting",
                                           @(QNConnectionStateReconnected) : @"Reconnected"
                                           };
    NSLog(@"roomStateDidChange - %@", connectionStateDictionary[@(state)]);
    dispatch_async(dispatch_get_main_queue(), ^{
        if (QNConnectionStateConnected == state) {
            self.videoButton.selected = YES;
            self.microphoneButton.selected = YES;
            // 音频
            self.audioTrack = [QNRTC createMicrophoneAudioTrack];
            self.audioTrack.delegate = self;
//                [self.audioTrack setVolume:0.5];
            
            // 发布音视频
            [self.rtcClient publish:@[self.cameraTrack,self.audioTrack] completeCallback:^(BOOL onPublished, NSError *error) {
                if (onPublished) {
                    NSLog(@"didPublishLocalTracks");
                    dispatch_async(dispatch_get_main_queue(), ^{
                        self.microphoneButton.enabled = YES;
                        self.videoButton.enabled = YES;
                    });
                }else {
                    NSLog(@"publish error: %@",error);
                }
            }];
        } else if (QNConnectionStateDisconnected == state) {
            self.videoButton.enabled = NO;
            self.videoButton.selected = NO;
        } else if (QNConnectionStateReconnecting == state) {
            self.videoButton.enabled = NO;
            self.microphoneButton.enabled = NO;
        } else if (QNConnectionStateReconnected == state) {
            self.videoButton.enabled = YES;
            self.microphoneButton.enabled = YES;
        }
    });
}
/*
 远端用户加入房间的回调
 */
- (void)RTCClient:(QNRTCClient *)client didJoinOfUserID:(NSString *)userID userData:(NSString *)userData {
    NSLog(@"didJoinOfUserId - userId %@ userData %@", userID, userData);
}

/*
 远端用户发布音/视频的回调
 */
- (void)RTCClient:(QNRTCClient *)client didUserPublishTracks:(NSArray<QNTrack *> *)tracks ofUserID:(NSString *)userID {
    NSLog(@"didUserPublishTracks - tracks %@ userId %@", tracks, userID);
}

/*
// 订阅远端用户成功的回调
// */
- (void)RTCClient:(QNRTCClient *)client didSubscribedRemoteVideoTracks:(NSArray<QNRemoteVideoTrack *> *)videoTracks audioTracks:(NSArray<QNRemoteAudioTrack *> *)audioTracks ofUserID:(NSString *)userID {
    NSLog(@"didSubscribedRemoteTracks -  %d,%d userTd %@", audioTracks.count, videoTracks.count, userID);
    dispatch_async(dispatch_get_main_queue(), ^{
        for (QNRemoteAudioTrack * audioTrack in audioTracks) {
            audioTrack.delegate = self;
        }
        for (QNRemoteVideoTrack * videoTrack in videoTracks) {
            [videoTrack play:[self remoteUserView:userID]];
            videoTrack.delegate = self;
        }
    });

}

/*
 远端用户取消发布音/视频的回调
 */
- (void)RTCClient:(QNRTCClient *)client didUserUnpublishTracks:(NSArray<QNTrack *> *)tracks ofUserID:(NSString *)userID {
    NSLog(@"didUserUnpublishTracks - tracks %@ userId %@", tracks, userID);
    
    for (QNTrack *track in tracks) {
        if ([track.tag isEqual:@"camera"]) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.remoteView removeFromSuperview];
            });
        }
    }
}

/*
 远端用户离开房间的回调
 */
- (void)RTCClient:(QNRTCClient *)client didLeaveOfUserID:(NSString *)userID {
    NSLog(@"didLeaveOfUserId - %@", userID);
}

/*
 远端用户发生重连的回调。
 */
- (void)RTCClient:(QNRTCClient *)client didReconnectingOfUserID:(NSString *)userID {
    NSLog(@"didReconnectingOfUserID - %@",userID);
}

/*
 远端用户重连成功的回调
 */
- (void)RTCClient:(QNRTCClient *)client didReconnectedOfUserID:(NSString *)userID {
    NSLog(@"didReconnectedOfUserID - %@",userID);
}

#pragma mark - QNRemoteVideoTrackDelegate
- (void)remoteVideoTrack:(QNRemoteVideoTrack *)remoteVideoTrack didGetPixelBuffer:(CVPixelBufferRef)pixelBuffer {
    NSLog(@"remoteVideoTrack: %@ tag: %@ didGetPixelBuffer",remoteVideoTrack.trackID,remoteVideoTrack.tag);
}

#pragma mark - QNRemoteAudioTrackDelegate
- (void)remoteAudioTrack:(QNRemoteAudioTrack *)remoteAudioTrack didGetAudioBuffer:(AudioBuffer *)audioBuffer bitsPerSample:(NSUInteger)bitsPerSample sampleRate:(NSUInteger)sampleRate {
    NSLog(@"remoteAudioTrack: %@ tag: %@ didGetAudioBuffer",remoteAudioTrack.trackID,remoteAudioTrack.tag);
}

#pragma mark - QNLocalVideoTrackDelegate
- (void)localVideoTrack:(QNLocalVideoTrack *)localVideoTrack didGetPixelBuffer:(CVPixelBufferRef)pixelBuffer {
//    NSLog(@"cameraVideoTrack: %@ tag: %@ didGetPixelBuffer",cameraVideoTrack.trackID,track.tag);

}

#pragma mark - QNLocalAudioTrackDelegate
- (void)localAudioTrack:(QNLocalAudioTrack *)localAudioTrack didGetAudioBuffer:(AudioBuffer *)audioBuffer bitsPerSample:(NSUInteger)bitsPerSample sampleRate:(NSUInteger)sampleRate {
//    NSLog(@"microphoneAudioTrack: %@ tag: %@ didGetAudioBuffer",track.trackID,track.tag);
}

#pragma mark - 远端用户画面
#warning 仅考虑单个远端的情况

- (QNVideoGLView *)remoteUserView:(NSString *)userId {
    self.remoteView = [[QNVideoGLView alloc] initWithFrame:CGRectMake(self.screenWidth - self.screenWidth/3, 20, self.screenWidth/3, self.screenWidth/27*16)];
    [self.preview addSubview:self.remoteView];
    
    UILabel *nameLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, self.screenWidth/27*8 - 30, self.screenWidth/3, 60)];
    nameLabel.text = userId;
    nameLabel.textColor = [UIColor whiteColor];
    nameLabel.font = [UIFont systemFontOfSize:15.0];
    nameLabel.textAlignment = NSTextAlignmentCenter;
    [self.remoteView addSubview:nameLabel];
    
    return self.remoteView;
}

#pragma mark - buttons action

- (void)closePublishAndBack:(UIButton *)button {
    [self dismissViewControllerAnimated:YES completion:^{
        [self.rtcClient leave];
        [QNRTC deinit];
    }];
}

- (void)videoButtonAction:(UIButton *)button {
    button.selected = !button.isSelected;
    if (button.isSelected) {
        [self.rtcClient publish:@[self.cameraTrack]];
    }else {
        [self.rtcClient unpublish:@[self.cameraTrack]];
    }
    
}

- (void)microphoneButtonAction:(UIButton *)button {
    button.selected = !button.isSelected;
    if (button.isSelected) {
        [self.rtcClient publish:@[self.audioTrack]];
    }else {
        [self.rtcClient unpublish:@[self.audioTrack]];
    }
}

- (void)cameraButtonAction:(UIButton *)button {
    button.selected = !button.isSelected;
    [self.cameraTrack switchCamera];
}
    
#pragma mark - layout view

- (void)layoutView {
    UIButton *closeButton = [[UIButton alloc] initWithFrame:CGRectMake(self.screenWidth/2 - 25, self.screenHeight - 90, 50, 50)];
    [closeButton setImage:[UIImage imageNamed:@"close-phone"] forState:UIControlStateNormal];
    [closeButton addTarget:self action:@selector(closePublishAndBack:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:closeButton];
    
    UIButton *videoButton = [[UIButton alloc] initWithFrame:CGRectMake(self.screenWidth/2 - 101, self.screenHeight - 160, 42, 42)];
    videoButton.enabled = NO;
    [videoButton setImage:[UIImage imageNamed:@"video-open"] forState:UIControlStateSelected];
    [videoButton setImage:[UIImage imageNamed:@"video-close"] forState:UIControlStateNormal];
    [videoButton addTarget:self action:@selector(videoButtonAction:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:videoButton];
    self.videoButton = videoButton;
    
    UIButton *microphoneButton = [[UIButton alloc] initWithFrame:CGRectMake(self.screenWidth/2 - 21, self.screenHeight - 160, 42, 42)];
    microphoneButton.enabled = NO;
    [microphoneButton setImage:[UIImage imageNamed:@"microphone"] forState:UIControlStateSelected];
    [microphoneButton setImage:[UIImage imageNamed:@"microphone-disable"] forState:UIControlStateNormal];
    [microphoneButton addTarget:self action:@selector(microphoneButtonAction:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:microphoneButton];
    self.microphoneButton = microphoneButton;
    
    UIButton *cameraButton = [[UIButton alloc] initWithFrame:CGRectMake(self.screenWidth/2 + 59, self.screenHeight - 160, 42, 42)];
    [cameraButton setImage:[UIImage imageNamed:@"camera-front"] forState:UIControlStateNormal];
    [cameraButton setImage:[UIImage imageNamed:@"camera-back"] forState:UIControlStateSelected];
    [cameraButton addTarget:self action:@selector(cameraButtonAction:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:cameraButton];
    self.cameraButton = cameraButton;
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
