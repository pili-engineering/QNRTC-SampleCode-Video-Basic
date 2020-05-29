//
//  RoomViewController.m
//  QNRTCSampleCodeOne
//
//  Created by 冯文秀 on 2019/1/28.
//  Copyright © 2019 Hera. All rights reserved.
//

#import "RoomViewController.h"
#import <QNRTCKit/QNRTCKit.h>
@interface RoomViewController ()
<
QNRTCEngineDelegate
>
@property (nonatomic, assign) CGFloat screenWidth;
@property (nonatomic, assign) CGFloat screenHeight;

@property (nonatomic, strong) UIButton *videoButton;
@property (nonatomic, strong) UIButton *microphoneButton;
@property (nonatomic, strong) UIButton *cameraButton;

@property (nonatomic, strong) NSDictionary *settingsDic;

@property (nonatomic, assign) CGSize videoEncodeSize;
@property (nonatomic, assign) NSInteger bitrate;

@property (nonatomic, strong) QNRTCEngine *rtcEngine;
@property (nonatomic, strong) QNTrackInfo *audioTrackInfo;
@property (nonatomic, strong) QNTrackInfo *cameraTrackInfo;
@end

@implementation RoomViewController

- (UIStatusBarStyle)preferredStatusBarStyle {
    return UIStatusBarStyleLightContent;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.view.backgroundColor = [UIColor colorWithRed:30/255.0 green:30/255.0 blue:30/255.0 alpha:1.0];
    
    self.screenWidth = CGRectGetWidth(self.view.frame);
    self.screenHeight = CGRectGetHeight(self.view.frame);
    
    // 获取 QNRTCKit 的分辨率、帧率、码率的配置
    self.settingsDic = [self settingsArrayAtIndex:1];
    
    // QNRTCKit 的分辨率
    self.videoEncodeSize = CGSizeFromString(_settingsDic[@"VideoSize"]);
    // QNRTCKit 的码率
    self.bitrate = [_settingsDic[@"Bitrate"] integerValue];
    
    // 配置 QNRTCKit
    [self configureRTCEngine];
    
    // 布局视图
    [self layoutView];
}

#pragma mark - settings

- (NSDictionary *)settingsArrayAtIndex:(NSInteger)index {
    NSArray *settingsArray = @[@{@"VideoSize":NSStringFromCGSize(CGSizeMake(288, 352)), @"FrameRate":@15, @"Bitrate":@(300*1000)},
                        @{@"VideoSize":NSStringFromCGSize(CGSizeMake(480, 640)), @"FrameRate":@15, @"Bitrate":@(400*1000) },
                        @{@"VideoSize":NSStringFromCGSize(CGSizeMake(544, 960)), @"FrameRate":@15, @"Bitrate":@(700*1000)},
                        @{@"VideoSize":NSStringFromCGSize(CGSizeMake(720, 1280)), @"FrameRate":@20, @"Bitrate":@(1000*1000)}];
    return settingsArray[index];
}

#pragma mark - QNRTCKit 核心类

- (void)configureRTCEngine {
    // QNRTCEngine 初始化
    self.rtcEngine = [[QNRTCEngine alloc] init];
    
    // 设置代理 QNRTCEngineDelegate
    self.rtcEngine.delegate = self;
    
    // 设置本地预览视图
    self.rtcEngine.previewView.frame = CGRectMake(0, 0, self.screenWidth, self.screenHeight);
    [self.view addSubview:_rtcEngine.previewView];
    
    // 设置采集视频的帧率
    self.rtcEngine.videoFrameRate = [self.settingsDic[@"FrameRate"] integerValue];
    
    // 开始采集
    [self.rtcEngine startCapture];
    
    // 加入房间
    [self.rtcEngine joinRoomWithToken:self.token];
}

#pragma mark - QNRTCEngineDelegate 代理回调

/*
 发生错误的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didFailWithError:(NSError *)error {
    NSLog(@"didFailWithError - %@", error);
    NSArray *errorArray = @[@(QNRTCErrorTokenError),
                            @(QNRTCErrorRoomInstanceClosed),
                            @(QNRTCErrorReconnectTokenError),
                            @(QNRTCErrorPublishStreamNotExist),
                            @(QNRTCErrorSubscribeStreamNotExist),
                            @(QNRTCErrorServerUnavailable),
                            @(QNRTCErrorInvalidParameter)];
    dispatch_async(dispatch_get_main_queue(), ^{
        switch (error.code) {
            case QNRTCErrorAuthFailed:
                NSLog(@"鉴权失败，请检查鉴权");
                break;
            case QNRTCErrorRoomIsFull:
                NSLog(@"房间人数已满");
                break;
            case QNRTCErrorTokenError:
                //关于 token 签算规则, 详情请参考【服务端开发说明.RoomToken 签发服务】https://doc.qnsdk.com/rtn/docs/server_overview#1
                NSLog(@"roomToken 错误");
                break;
            case QNRTCErrorTokenExpired:
                NSLog(@"roomToken 过期");
                break;
            case QNRTCErrorUserAlreadyExist:
                NSLog(@"用户已存在");
                break;
            case QNRTCErrorNoPermission:
                NSLog(@"请检查用户是否有权限，如:合流");
                break;
            case QNRTCErrorReconnectTokenError:
                NSLog(@"重新进入房间超时，请务必调用 leaveRoom, 重新进入房间");
                break;
            case QNRTCErrorPublishFailed:
                NSLog(@"发布失败，请查看是否加入房间，并确定对于音频/视频 Track，分别最多只能有一路为 master");
                break;
            case QNRTCErrorInvalidParameter:
                NSLog(@"服务交互参数错误，请在开发时注意合流、踢人动作等参数的设置");
                break;
            case QNRTCErrorRoomClosed:
                NSLog(@"房间已被管理员关闭");
                break;
                
            default:
                break;
        }
        if ([errorArray containsObject:@(error.code)] ) {
            UIAlertController *alertVc = [UIAlertController alertControllerWithTitle:@"提示" message:[NSString stringWithFormat:@"error code: %ld error domain: %@", (long)error.code, error.domain] preferredStyle:UIAlertControllerStyleAlert];
            UIAlertAction *sureAction = [UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
                [self dismissViewControllerAnimated:YES completion:nil];
            }];
            [alertVc addAction:sureAction];
            [self presentViewController:alertVc animated:YES completion:nil];
        }
    });
}

/*
 房间内状态变化的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine roomStateDidChange:(QNRoomState)roomState {
    NSDictionary *roomStateDictionary =  @{@(QNRoomStateIdle) : @"Idle",
                                           @(QNRoomStateConnecting) : @"Connecting",
                                           @(QNRoomStateConnected): @"Connected",
                                           @(QNRoomStateReconnecting) : @"Reconnecting",
                                           @(QNRoomStateReconnected) : @"Reconnected"
                                           };
    NSLog(@"roomStateDidChange - %@", roomStateDictionary[@(roomState)]);
    dispatch_async(dispatch_get_main_queue(), ^{
        if (QNRoomStateConnected == roomState) {
            self.videoButton.selected = YES;
            self.microphoneButton.selected = YES;
            // 音频
            QNTrackInfo *audioTrack = [[QNTrackInfo alloc] initWithSourceType:QNRTCSourceTypeAudio master:YES];
            // 视频
            QNTrackInfo *cameraTrack = [[QNTrackInfo alloc] initWithSourceType:QNRTCSourceTypeCamera tag:@"camera" master:YES bitrateBps:self.bitrate videoEncodeSize:self.videoEncodeSize];
            
            // 发布音视频
            [self.rtcEngine publishTracks:@[audioTrack, cameraTrack]];
            
        } else if (QNRoomStateIdle == roomState) {
            self.videoButton.enabled = NO;
            self.videoButton.selected = NO;
        } else if (QNRoomStateReconnecting == roomState) {
            self.videoButton.enabled = NO;
            self.microphoneButton.enabled = NO;
        } else if (QNRoomStateReconnected == roomState) {
            self.videoButton.enabled = YES;
            self.microphoneButton.enabled = YES;
        }
    });
}

/*
 发布本地音/视频成功的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didPublishLocalTracks:(NSArray<QNTrackInfo *> *)tracks {
    NSLog(@"didPublishLocalTracks - %@", tracks);
    dispatch_async(dispatch_get_main_queue(), ^{
        for (QNTrackInfo *trackInfo in tracks) {
            if (trackInfo.kind == QNTrackKindAudio) {
                self.microphoneButton.enabled = YES;
                self.audioTrackInfo = trackInfo;
                continue;
            }
            if (trackInfo.kind == QNTrackKindVideo) {
                if ([trackInfo.tag isEqualToString:@"camera"]) {
                    self.videoButton.enabled = YES;
                    self.cameraTrackInfo = trackInfo;
                }
                continue;
            }
        }
    });
}

/*
 远端用户加入房间的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didJoinOfRemoteUserId:(NSString *)userId userData:(NSString *)userData {
    NSLog(@"didJoinOfRemoteUserId - userId %@ userData %@", userId, userData);
}

/*
 远端用户发布音/视频的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didPublishTracks:(NSArray<QNTrackInfo *> *)tracks ofRemoteUserId:(NSString *)userId {
    NSLog(@"didPublishTracks - tracks %@ userId %@", tracks, userId);
}

/*
 订阅远端用户成功的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didSubscribeTracks:(NSArray<QNTrackInfo *> *)tracks ofRemoteUserId:(NSString *)userId {
    NSLog(@"didSubscribeTracks - tracks %@ userTd %@", tracks, userId);
}

/*
 远端用户音频状态变更的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didAudioMuted:(BOOL)muted ofTrackId:(NSString *)trackId byRemoteUserId:(NSString *)userId {
    NSLog(@"didAudioMuted - muted %d trackId %@ userId %@", muted, trackId, userId);
}

/*
 远端用户音频状态变更的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didVideoMuted:(BOOL)muted ofTrackId:(nonnull NSString *)trackId byRemoteUserId:(nonnull NSString *)userId {
    NSLog(@"didVideoMuted - muted %d trackId %@ userId %@", muted, trackId, userId);
}

/*
 被踢出房间的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didKickoutByUserId:(NSString *)userId {
    NSLog(@"didKickoutByUserId - %@", userId);
    dispatch_async(dispatch_get_main_queue(), ^{
        [self dismissViewControllerAnimated:YES completion:nil];
    });
}

/*
 远端用户取消发布音/视频的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didUnPublishTracks:(NSArray<QNTrackInfo *> *)tracks ofRemoteUserId:(NSString *)userId {
    NSLog(@"didUnPublishTracks - tracks %@ userId %@", tracks, userId);
}

/*
 远端用户离开房间的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didLeaveOfRemoteUserId:(NSString *)userId {
    NSLog(@"didLeaveOfRemoteUserId - %@", userId);
}

/*
 远端用户首帧解码后的回调（仅在远端用户发布视频时会回调）
 */
- (QNVideoRender *)RTCEngine:(QNRTCEngine *)engine firstVideoDidDecodeOfTrackId:(NSString *)trackId remoteUserId:(NSString *)userId {
    QNVideoRender *render = [[QNVideoRender alloc] init];
    render.renderView = [self remoteUserView:userId];
    return render;
}

/*
 远端用户取消渲染的回调
 */
- (void)RTCEngine:(QNRTCEngine *)engine didDetachRenderView:(UIView *)renderView ofTrackId:(NSString *)trackId remoteUserId:(NSString *)userId {
    NSLog(@"didDetachRenderView - renderview %@ trackId %@ userId %@", renderView, trackId, userId);
    for (UIView *view in self.rtcEngine.previewView.subviews) {
        if ([view isEqual:renderView]) {
            [view removeFromSuperview];
            return;
        }
    }
}

#pragma mark - 远端用户画面
#warning 仅考虑单个远端的情况

- (QNVideoView *)remoteUserView:(NSString *)userId {
    QNVideoView *remoteView = [[QNVideoView alloc] initWithFrame:CGRectMake(self.screenWidth - self.screenWidth/3, 20, self.screenWidth/3, self.screenWidth/27*16)];
    [self.rtcEngine.previewView addSubview:remoteView];
    
    UILabel *nameLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, self.screenWidth/27*8 - 30, self.screenWidth/3, 60)];
    nameLabel.text = userId;
    nameLabel.textColor = [UIColor whiteColor];
    nameLabel.font = [UIFont systemFontOfSize:15.0];
    nameLabel.textAlignment = NSTextAlignmentCenter;
    [remoteView addSubview:nameLabel];
    
    return remoteView;
}

#pragma mark - buttons action

- (void)closePublishAndBack:(UIButton *)button {
    [self dismissViewControllerAnimated:YES completion:^{
        [self.rtcEngine leaveRoom];
        [self.rtcEngine stopCapture];
    }];
}

- (void)videoButtonAction:(UIButton *)button {
    button.selected = !button.isSelected;
    [self.rtcEngine muteVideo:!button.isSelected];
}

- (void)microphoneButtonAction:(UIButton *)button {
    button.selected = !button.isSelected;
    [self.rtcEngine muteAudio:!button.isSelected];
}

- (void)cameraButtonAction:(UIButton *)button {
    button.selected = !button.isSelected;
    [self.rtcEngine toggleCamera];
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
