package com.qiniu.droid.rtc.sample1v1;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.qiniu.droid.rtc.QNFileLogHelper;
import com.qiniu.droid.rtc.sample1v1.utils.ToastUtils;
import com.qiniu.droid.rtc.sample1v1.utils.Utils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import java.io.File;

import xcrash.TombstoneManager;
import xcrash.XCrash;

public class RTCApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化使用的第三方二维码扫描库，与 QNRTC 无关，请忽略
        ZXingLibrary.initDisplayOpinion(getApplicationContext());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        xcrash.XCrash.init(this, new XCrash.InitParameters()
                .setLogDir(getExternalFilesDir(null).getAbsolutePath())
                .setJavaDumpNetworkInfo(false)
                .setNativeDumpNetwork(false)
                .setNativeDumpAllThreads(false)
                .setAppVersion(Utils.getVersion(this)));
        checkToUploadCrashFiles();
    }

    private void checkToUploadCrashFiles() {
        File crashFolder = new File(getExternalFilesDir(null).getAbsolutePath());
        File[] crashFiles = crashFolder.listFiles();
        if (crashFiles == null) {
            return;
        }
        for (File crashFile : crashFiles) {
            if (crashFile.isFile()) {
                QNFileLogHelper.getInstance().reportLogFileByPath(crashFile.getPath(), new QNFileLogHelper.LogReportCallback() {
                    @Override
                    public void onReportSuccess(String name) {
                        ToastUtils.s(RTCApplication.this, "崩溃日志已上传！");
                        TombstoneManager.deleteTombstone(crashFile.getPath());
                    }

                    @Override
                    public void onReportError(String name, String errorMsg) {
                        ToastUtils.s(RTCApplication.this, "崩溃日志上传失败 : " + errorMsg);
                    }
                });
            }
        }
    }
}
