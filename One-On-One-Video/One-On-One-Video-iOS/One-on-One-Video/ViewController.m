//
//  ViewController.m
//  QNRTCSampleCodeOne
//
//  Created by 冯文秀 on 2019/1/28.
//  Copyright © 2019 Hera. All rights reserved.
//

#import "ViewController.h"
#import "RoomViewController.h"
#import "ScanViewController.h"
#import <QNRTCKit/QNRTCKit.h>
@interface ViewController ()
<
UITextFieldDelegate,
ScanViewControlerDelegate
>
@property (nonatomic, strong) UITextField *tokenTextField;
@property (nonatomic, strong) UIButton *joinButton;

@end

@implementation ViewController

- (UIStatusBarStyle)preferredStatusBarStyle {
    return UIStatusBarStyleLightContent;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    self.view.backgroundColor = [UIColor colorWithRed:30/255.0 green:30/255.0 blue:30/255.0 alpha:1.0];
    
    // 布局视图
    [self layoutView];
}

#pragma mark - buttons action

- (void)offerHelpAboutToken:(UIButton *)button {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:@""] options:@{} completionHandler:nil];
    });
}

- (void)scanCodeForToken:(UIButton *)button {
    ScanViewController *scanVC = [[ScanViewController alloc] init];
    scanVC.delegate = self;
    scanVC.modalPresentationStyle = UIModalPresentationFullScreen;
    [self presentViewController:scanVC animated:YES completion:nil];
}

- (void)joinInRoom:(UIButton *)button {
    [self.view endEditing:YES];

    if (self.tokenTextField.text.length != 0) {
        RoomViewController *roomVC = [[RoomViewController alloc] init];
        roomVC.token = self.tokenTextField.text;
        roomVC.modalPresentationStyle = UIModalPresentationFullScreen;
        [self presentViewController:roomVC animated:YES completion:nil];
    }
}

#pragma mark - ScanViewControlerDelegate

- (void)scanCodeResult:(NSString *)string {
    if (string.length != 0) {
        self.tokenTextField.text = string;
        self.joinButton.enabled = YES;
        self.joinButton.backgroundColor = [UIColor colorWithRed:3/255.0 green:160/255.0 blue:222/255.0 alpha:1.0];
    }
}

#pragma mark - layout view

- (void)layoutView {
    CGFloat screenWidth = CGRectGetWidth(self.view.frame);
    CGFloat screenHeight = CGRectGetHeight(self.view.frame);
    
    // 标题 label
    UILabel *titleLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, 70.0, screenWidth, 30.0)];
    titleLabel.text = @"Basic Sample Code";
    titleLabel.font = [UIFont fontWithName:@"HelveticaNeue-Bold" size:16.0];
    titleLabel.textColor = [UIColor whiteColor];
    titleLabel.textAlignment = NSTextAlignmentCenter;
    [self.view addSubview:titleLabel];
    
    // 名称 label
    UILabel *nameLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, 120.0, screenWidth, 30.0)];
    nameLabel.text = @"one to one";
    nameLabel.font = [UIFont fontWithName:@"HelveticaNeue-Medium" size:14.0];
    nameLabel.textColor = [UIColor colorWithRed:155/255.0 green:155/255.0 blue:155/255.0 alpha:1.0];
    nameLabel.textAlignment = NSTextAlignmentCenter;
    [self.view addSubview:nameLabel];
    
    // token label
    UILabel *tokenLabel = [[UILabel alloc] initWithFrame:CGRectMake(40, 200.0, 90, 30.0)];
    tokenLabel.text = @"Room Token";
    tokenLabel.textColor = [UIColor whiteColor];
    tokenLabel.font = [UIFont fontWithName:@"HelveticaNeue-Medium" size:15.0];
    tokenLabel.textAlignment = NSTextAlignmentLeft;
    [self.view addSubview:tokenLabel];
    
    // 帮助 button
    UIButton *helpButton = [[UIButton alloc] initWithFrame:CGRectMake(140, 205, 20.0, 20.0)];
    [helpButton setImage:[UIImage imageNamed:@"help"] forState:UIControlStateNormal];
    [helpButton addTarget:self action:@selector(offerHelpAboutToken:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:helpButton];
    
    // token textField
    UITextField *tokenTextField = [[UITextField alloc] initWithFrame:CGRectMake(40, 250, screenWidth - 120, 32.0)];
    tokenTextField.delegate = self;
    tokenTextField.placeholder = @" please input token of room";
    tokenTextField.backgroundColor = [UIColor colorWithRed:142/255.0 green:142/255.0 blue:142/255.0 alpha:1.0];
    tokenTextField.font = [UIFont systemFontOfSize:14];
    [self.view addSubview:tokenTextField];
    self.tokenTextField = tokenTextField;
    
    // 扫描 button
    UIButton *scanButton = [[UIButton alloc] initWithFrame:CGRectMake(screenWidth - 70, 250, 32.0, 32.0)];
    [scanButton setImage:[UIImage imageNamed:@"scan"] forState:UIControlStateNormal];
    [scanButton addTarget:self action:@selector(scanCodeForToken:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:scanButton];
    
    // 加入 button
    UIButton *joinButton = [[UIButton alloc] initWithFrame:CGRectMake(90, 385, screenWidth - 180, 32.0)];
    joinButton.enabled = NO;
    joinButton.backgroundColor = [UIColor lightGrayColor];
    [joinButton setTitle:@"Join In Room" forState:UIControlStateNormal];
    [joinButton addTarget:self action:@selector(joinInRoom:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:joinButton];
    self.joinButton = joinButton;
    
    // 版本 label
    UILabel *versionLabel = [[UILabel alloc] initWithFrame:CGRectMake(40, screenHeight - 130, screenWidth - 80, 60)];
    NSArray *infoArray = [[QNRTCEngine versionInfo] componentsSeparatedByString:@"-"];
    versionLabel.text = [NSString stringWithFormat:@"版本：v%@\n\n更新时间：%@-%@-%@", infoArray[0], infoArray[1], infoArray[2], infoArray[3]];
    versionLabel.textColor = [UIColor colorWithRed:155/255.0 green:155/255.0 blue:155/255.0 alpha:1.0];
    versionLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:13.0];
    versionLabel.textAlignment = NSTextAlignmentRight;
    versionLabel.numberOfLines = 0;
    [self.view addSubview:versionLabel];
}

#pragma mark - textField delegate

- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    if (textField.text.length != 0) {
        self.joinButton.enabled = YES;
        self.joinButton.backgroundColor = [UIColor colorWithRed:3/255.0 green:160/255.0 blue:222/255.0 alpha:1.0];
    }
    return YES;
}

- (void)textFieldDidEndEditing:(UITextField *)textField {
    if (textField.text.length != 0) {
        self.joinButton.enabled = YES;
        self.joinButton.backgroundColor = [UIColor colorWithRed:3/255.0 green:160/255.0 blue:222/255.0 alpha:1.0];
    } else{
        self.joinButton.enabled = NO;
        self.joinButton.backgroundColor = [UIColor grayColor];
    }
}

#pragma mark - 点击空白回收键盘

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}

@end
