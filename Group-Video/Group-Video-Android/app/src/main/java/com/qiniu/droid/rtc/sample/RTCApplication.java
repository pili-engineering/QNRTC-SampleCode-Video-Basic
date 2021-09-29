package com.qiniu.droid.rtc.sample;

import android.app.Application;

import com.uuzuche.lib_zxing.activity.ZXingLibrary;

public class RTCApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化使用的第三方二维码扫描库，与 QNRTC 无关，请忽略
        ZXingLibrary.initDisplayOpinion(getApplicationContext());
    }
}
