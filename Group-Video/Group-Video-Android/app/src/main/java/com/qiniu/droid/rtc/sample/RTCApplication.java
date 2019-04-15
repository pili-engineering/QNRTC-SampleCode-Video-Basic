package com.qiniu.droid.rtc.sample;

import android.app.Application;

import com.qiniu.droid.rtc.QNLogLevel;
import com.qiniu.droid.rtc.QNRTCEnv;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

public class RTCApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 设置 QNRTC Log 输出等级
        QNRTCEnv.setLogLevel(QNLogLevel.INFO);

        // SDK 内部有 Log 保存接口，如有需要可开启后将 Log 文件保存到本地
        // QNRTCEnv.setLogFileEnabled(true);
        // 设置保存文件的最大数量，达到该数量时会自动覆盖，默认值为 3。
        // 如果存在 SDCard 且具有权限，将保存在 /sdcard/Android/data/包名/files/QNRTCLog 中，
        // 若没有，将保存在 mContext.getFilesDir().getAbsolutePath() + File.separator + "QNRTCLog" 中
        // QNRTCEnv.setLogFileMaxCount(3);

        // 初始化 QNRTC 环境
        QNRTCEnv.init(this);

        // 初始化使用的第三方二维码扫描库，与 QNRTC 无关，请忽略
        ZXingLibrary.initDisplayOpinion(getApplicationContext());
    }
}
