//
//  ScanViewController.h
//  QNRTCSampleCodeOne
//
//  Created by 冯文秀 on 2019/1/29.
//  Copyright © 2019 Hera. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@protocol ScanViewControlerDelegate <NSObject>

/**
 扫描结果的回调

 @param string 扫描得到的结果，字符串类型
 */
- (void)scanCodeResult:(NSString *)string;

@end

@interface ScanViewController : UIViewController

@property (nonatomic, strong) id<ScanViewControlerDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
