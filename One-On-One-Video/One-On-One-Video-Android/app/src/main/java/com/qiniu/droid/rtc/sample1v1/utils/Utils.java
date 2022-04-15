package com.qiniu.droid.rtc.sample1v1.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;

import java.io.IOException;
import java.net.InetAddress;

public class Utils {
    /**
     * 创建自定义 dns manager
     *
     * 注意：该方法需要在子线程中调用，否则可能会抛异常
     *
     * @param context 上下文
     * @return dns manager
     */
    public static DnsManager getDefaultDnsManager(Context context) {
        IResolver r0 = null;
        try {
            // 默认使用阿里云公共 DNS 服务，避免系统 DNS 解析可能出现的跨运营商、重定向等问题，详情可参考 https://www.alidns.com/
            // 超时时间参数可选，不指定默认为 10s 的超时
            // 超时时间单位：s
            r0 = new Resolver(InetAddress.getByName("223.5.5.5"), 3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 默认 Dnspod 服务，使用腾讯公共 DNS 服务，详情可参考 https://www.dnspod.cn/Products/Public.DNS
        // 超时时间参数可选，不指定默认为 10s 的超时
        // 超时时间单位：s
        IResolver r1 = new DnspodFree("119.29.29.29", 3);
        // 系统默认 DNS 解析，可能会出现解析跨运营商等问题
        IResolver r2 = AndroidDnsServer.defaultResolver(context);
        return new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
    }

    public static String getVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
}