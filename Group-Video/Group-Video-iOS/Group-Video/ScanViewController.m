//
//  ScanViewController.m
//  QNRTCSampleCodeGroup
//
//  Created by 冯文秀 on 2019/1/29.
//  Copyright © 2019 Hera. All rights reserved.
//

#import "ScanViewController.h"
#import <AVFoundation/AVFoundation.h>
@interface ScanViewController ()
<
AVCaptureMetadataOutputObjectsDelegate
>
@property (nonatomic, strong) UIView *boxView;
@property (nonatomic, strong) CALayer *scanLayer;
@property (nonatomic, strong) AVCaptureSession *captureSession;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *videoPreviewLayer;

@property (nonatomic, strong) NSString *scanResult;
@property (nonatomic, strong) NSTimer *timer;
@end

@implementation ScanViewController

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self stopScanCode];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    [self startScanCode];
    
    [self layoutView];
}

- (BOOL)startScanCode {
    NSError *error;
    
    // 初始化捕捉设备（AVCaptureDevice），类型为 AVMediaTypeVideo
    AVCaptureDevice *captureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    
    // 用 captureDevice 创建输入流
    AVCaptureDeviceInput *input = [AVCaptureDeviceInput deviceInputWithDevice:captureDevice error:&error];
    if (!input) {
        NSLog(@"%@", [error localizedDescription]);
        return NO;
    }
    
    // 创建媒体数据输出流
    AVCaptureMetadataOutput *captureMetadataOutput = [[AVCaptureMetadataOutput alloc] init];
    
    // 实例化捕捉会话
    self.captureSession = [[AVCaptureSession alloc] init];
    
    // 将添加输入流和媒体输出流到会话
    [self.captureSession addInput:input];
    [self.captureSession addOutput:captureMetadataOutput];
    
    // 创建串行队列，并加媒体输出流添加到队列当中
    dispatch_queue_t dispatchQueue;
    dispatchQueue = dispatch_queue_create("myQueue", NULL);
    [captureMetadataOutput setMetadataObjectsDelegate:self queue:dispatchQueue];
    
    // 设置输出媒体数据类型为 QRCode
    [captureMetadataOutput setMetadataObjectTypes:[NSArray arrayWithObject:AVMetadataObjectTypeQRCode]];
    
    // 实例化预览图层
    self.videoPreviewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:self.captureSession];
    
    // 设置预览图层填充方式
    [self.videoPreviewLayer setVideoGravity:AVLayerVideoGravityResizeAspectFill];
    [self.videoPreviewLayer setFrame:self.view.layer.bounds];
    [self.view.layer addSublayer:_videoPreviewLayer];
    
    // 设置扫描范围
    captureMetadataOutput.rectOfInterest = CGRectMake(0.2f, 0.2f, 0.8f, 0.8f);
    
    // 扫描框
    CGSize size = self.view.bounds.size;
    self.boxView = [[UIView alloc] initWithFrame:CGRectMake(size.width * 0.2f, (size.height - (size.width - size.width * 0.4f))/2, size.width - size.width * 0.4f, size.width - size.width * 0.4f)];
    self.boxView.layer.borderColor = [UIColor greenColor].CGColor;
    self.boxView.layer.borderWidth = 1.0f;
    
    [self.view addSubview:_boxView];
    
    // 扫描线
    self.scanLayer = [[CALayer alloc] init];
    self.scanLayer.frame = CGRectMake(0, 0, self.boxView.bounds.size.width, 1);
    self.scanLayer.backgroundColor = [UIColor colorWithRed:16/255.0 green:169/255.0 blue:235/255.0 alpha:1.0].CGColor;
    
    [self.boxView.layer addSublayer:_scanLayer];
    
    self.timer = [NSTimer scheduledTimerWithTimeInterval:0.2f target:self selector:@selector(moveScanLayer:) userInfo:nil repeats:YES];
    [self.timer fire];
    
    // 开始扫描
    [self.captureSession startRunning];
    return YES;
}

- (void)stopScanCode {
    [self.captureSession stopRunning];
    self.captureSession = nil;
    [self.scanLayer removeFromSuperlayer];
    [self.videoPreviewLayer removeFromSuperlayer];
}

- (void)moveScanLayer:(NSTimer *)timer {
    CGRect layerFrame = self.scanLayer.frame;
    if (self.boxView.frame.size.height < self.scanLayer.frame.origin.y) {
        layerFrame.origin.y = 0;
        self.scanLayer.frame = layerFrame;
    }else{
        layerFrame.origin.y += 5;
        [UIView animateWithDuration:0.1 animations:^{
            self.scanLayer.frame = layerFrame;
        }];
    }
}

#pragma mark - AVCaptureMetadataOutputObjectsDelegate

- (void)captureOutput:(AVCaptureOutput *)captureOutput didOutputMetadataObjects:(NSArray *)metadataObjects fromConnection:(AVCaptureConnection *)connection {
    // 判断是否有数据
    if (metadataObjects != nil && [metadataObjects count] > 0) {
        AVMetadataMachineReadableCodeObject *metadataObj = [metadataObjects objectAtIndex:0];
        // 判断回传的数据类型
        if ([[metadataObj type] isEqualToString:AVMetadataObjectTypeQRCode]) {
            NSLog(@"input code: %@", [metadataObj stringValue]);
            self.scanResult = [metadataObj stringValue];
            [self performSelectorOnMainThread:@selector(stopScanCode) withObject:nil waitUntilDone:NO];
            dispatch_async(dispatch_get_main_queue(), ^{
                UIAlertController *alertVc = [UIAlertController alertControllerWithTitle:@"提示" message:self.scanResult preferredStyle:UIAlertControllerStyleAlert];
                UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
                    [self startScanCode];
                }];
                UIAlertAction *sureAction = [UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
                    if (self.delegate && [self.delegate respondsToSelector:@selector(scanCodeResult:)]) {
                        [self.delegate scanCodeResult:self.scanResult];
                    }
                    [self.timer invalidate];
                    [self dismissViewControllerAnimated:YES completion:nil];
                }];
                [alertVc addAction:cancelAction];
                [alertVc addAction:sureAction];
                [self presentViewController:alertVc animated:YES completion:nil];
            });
        }
    }
}

#pragma mark - layout view

- (void)layoutView {
    CGFloat screenWidth = CGRectGetWidth(self.view.frame);
    
    UILabel *titleLab = [[UILabel alloc] initWithFrame:CGRectMake(0, 50, screenWidth, 40)];
    titleLab.font = [UIFont systemFontOfSize:16.0];
    titleLab.text = @"token 二维码扫描";
    titleLab.textAlignment = NSTextAlignmentCenter;
    titleLab.textColor = [UIColor whiteColor];
    [self.view addSubview:titleLab];
    
    UIButton *closeButton = [[UIButton alloc] initWithFrame:CGRectMake(30, 53, 34, 34)];
    closeButton.layer.cornerRadius = 17;
    closeButton.backgroundColor = [UIColor grayColor];
    [closeButton addTarget:self action:@selector(closeButtonSelected:) forControlEvents:UIControlEventTouchDown];
    [closeButton setImage:[UIImage imageNamed:@"back"] forState:UIControlStateNormal];
    [self.view addSubview:closeButton];
}

#pragma mark - close back

- (void)closeButtonSelected:(UIButton *)button {
    [self.timer invalidate];
    [self dismissViewControllerAnimated:YES completion:nil];
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
