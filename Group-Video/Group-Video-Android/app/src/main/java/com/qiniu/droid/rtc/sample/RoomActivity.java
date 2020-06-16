package com.qiniu.droid.rtc.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.qiniu.droid.rtc.QNCameraSwitchResultCallback;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNErrorCode;
import com.qiniu.droid.rtc.QNRTCEngine;
import com.qiniu.droid.rtc.QNRTCEngineEventListener;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.QNTrackInfo;
import com.qiniu.droid.rtc.QNTrackKind;
import com.qiniu.droid.rtc.QNVideoFormat;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomActivity extends AppCompatActivity implements QNRTCEngineEventListener {
    private static final String TAG = "RoomActivity";

    private GridLayout mVideoSurfaceViewGroup;
    private CustomVideoView mLocalVideoView;
    private QNRTCEngine mEngine;
    private Map<String, CustomVideoView> mVideoViewMap = new HashMap<>();
    private String mRoomToken;

    private boolean mIsMuteVideo = false;
    private boolean mIsMuteAudio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        mVideoSurfaceViewGroup = findViewById(R.id.video_surface_view_group);

        Intent intent = getIntent();
        mRoomToken = intent.getStringExtra("roomToken");

        QNRTCSetting setting = new QNRTCSetting();

        // 配置默认摄像头 ID，此处配置为前置摄像头
        setting.setCameraID(QNRTCSetting.CAMERA_FACING_ID.FRONT);

        // 配置音视频数据的编码方式，此处配置为硬编
        setting.setHWCodecEnabled(true);

        // 分辨率、码率、帧率配置为 640x480、15fps、400kbps
        setting.setVideoPreviewFormat(new QNVideoFormat(640, 480, 15))
                .setVideoEncodeFormat(new QNVideoFormat(640, 480, 15))
                .setVideoBitrate(400 * 1000);

        // 分辨率、码率、帧率配置为 352x288、15fps、300kbps
        //  setting.setVideoPreviewFormat(new QNVideoFormat(352, 288, 15))
        //          .setVideoEncodeFormat(new QNVideoFormat(352, 288, 15))
        //          .setVideoBitrate(300 * 1000);

        // 分辨率、码率、帧率配置为 960x544、15fps、700kbps
        // setting.setVideoPreviewFormat(new QNVideoFormat(960, 544, 15))
        //          .setVideoEncodeFormat(new QNVideoFormat(960, 544, 15))
        //          .setVideoBitrate(700 * 1000);

        // 分辨率、码率、帧率配置为 1280x720、15fps、1000kbps
        //  setting.setVideoPreviewFormat(new QNVideoFormat(1280, 720, 15))
        //          .setVideoEncodeFormat(new QNVideoFormat(1280, 720, 15))
        //          .setVideoBitrate(1000 * 1000);

        // 以上为我们的推荐配置组合，可结合实际情况选用，若需使用多 Track 可直接在创建 Track 时分开单独配置，即：
        // QNTrackInfo track = mEngine.createTrackInfoBuilder()
        //         .setVideoPreviewFormat(new QNVideoFormat(640, 480, 15))
        //         .setBitrate(400 * 1000)
        //         .setSourceType(QNSourceType.VIDEO_CAMERA)
        //         .create();

        // 创建 QNRTCEngine
        mEngine = QNRTCEngine.createEngine(getApplicationContext(), setting);

        // 设置回调监听
        mEngine.setEventListener(this);

        // 设置预览窗口
        mLocalVideoView = new CustomVideoView(this);
        mVideoSurfaceViewGroup.addView(mLocalVideoView);
        mEngine.setCapturePreviewWindow(mLocalVideoView.getVideoSurfaceView());

        // 加入房间，加入房间过程中会触发 QNRTCEngineEventListener#onRoomStateChanged 回调
        mEngine.joinRoom(mRoomToken);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 需要销毁 QNRTCEngine 以释放资源
        mEngine.destroy();
    }

    /**
     * 房间状态改变时会回调此方法
     * 房间状态回调只需要做提示用户，或者更新相关 UI； 不需要再做加入房间或者重新发布等其他操作！
     * @param state 房间状态，可参考 {@link QNRoomState}
     */
    @Override
    public void onRoomStateChanged(QNRoomState state) {
        switch (state) {
            case IDLE:
                // 初始化状态
                Log.i(TAG, "IDLE");
                break;
            case CONNECTING:
                // 正在连接
                Log.i(TAG, "CONNECTING");
                break;
            case CONNECTED:
                // 连接成功，即加入房间成功
                Log.i(TAG, "CONNECTED");
                // 加入房间成功后发布音视频数据，发布成功会触发 QNRTCEngineEventListener#onLocalPublished 回调
                mEngine.publish();
                break;
            case RECONNECTING:
                // 正在重连，若在通话过程中出现一些网络问题则会触发此状态
                Log.i(TAG, "RECONNECTING");
                break;
            case RECONNECTED:
                // 重连成功
                Log.i(TAG, "RECONNECTED");
                break;
        }
    }

    @Override
    public void onRoomLeft() {

    }

    @Override
    public void onRemoteUserJoined(String remoteUserId, String userData) {

    }

    @Override
    public void onRemoteUserLeft(String remoteUserId) {

    }

    /**
     * 本地 tracks 成功发布时会回调此方法
     *
     * @param trackInfoList 已发布的 tracks 列表
     */
    @Override
    public void onLocalPublished(List<QNTrackInfo> trackInfoList) {

    }

    /**
     * 远端用户 tracks 成功发布时会回调此方法
     *
     * @param remoteUserId 远端用户 userId
     * @param trackInfoList 远端用户发布的 tracks 列表
     */
    @Override
    public void onRemotePublished(String remoteUserId, List<QNTrackInfo> trackInfoList) {

    }

    @Override
    public void onRemoteUnpublished(String remoteUserId, List<QNTrackInfo> trackInfoList) {
        // 当远端视频取消发布时删除远端窗口
        CustomVideoView remoteVideoView = mVideoViewMap.get(remoteUserId);
        if (remoteVideoView != null) {
            mVideoSurfaceViewGroup.removeView(remoteVideoView);
            mVideoViewMap.remove(remoteUserId);
        }
    }

    @Override
    public void onRemoteUserMuted(String remoteUserId, List<QNTrackInfo> trackInfoList) {

    }

    @Override
    public void onSubscribed(String remoteUserId, List<QNTrackInfo> trackInfoList) {
        if (mVideoViewMap.get(remoteUserId) != null) {
            // 处理掉重连后的订阅渲染到窗口
            return;
        }
        // 筛选出视频 Track 以渲染到窗口
        for (QNTrackInfo track : trackInfoList) {
            if (track.getTrackKind().equals(QNTrackKind.VIDEO)) {
                // 设置渲染窗口
                CustomVideoView remoteVideoView = new CustomVideoView(this);
                mVideoSurfaceViewGroup.addView(remoteVideoView);
                mEngine.setRenderWindow(track, remoteVideoView.getVideoSurfaceView());
                remoteVideoView.setUserId(remoteUserId);
                mVideoViewMap.put(remoteUserId, remoteVideoView);
            }
        }
    }

    @Override
    public void onKickedOut(String userId) {

    }

    /**
     * 当媒体状态更新时会回调此方法
     *
     * @param report 媒体信息，详情请参考 {@link QNStatisticsReport}
     */
    @Override
    public void onStatisticsUpdated(final QNStatisticsReport report) {
        if (QNTrackKind.AUDIO.equals(report.trackKind)) {
            final String log = "音频码率:" + report.audioBitrate / 1000 + "kbps \n" +
                    "音频丢包率:" + report.audioPacketLostRate;
            Log.i(TAG, "媒体质量:" + log);
        } else if (QNTrackKind.VIDEO.equals(report.trackKind)) {
            final String log = "视频码率:" + report.videoBitrate / 1000 + "kbps \n" +
                    "视频丢包率:" + report.videoPacketLostRate + " \n" +
                    "视频的宽:" + report.width + " \n" +
                    "视频的高:" + report.height + " \n" +
                    "视频的帧率:" + report.frameRate;
            Log.i(TAG, "媒体质量:" + log);
        }
    }

    /**
     * 当收到远端用户的媒体状态时会回调此方法
     *
     * @param reports 媒体信息，详情请参考 {@link QNStatisticsReport}
     */
    @Override
    public void onRemoteStatisticsUpdated(List<QNStatisticsReport> reports) {
        for (QNStatisticsReport report : reports) {
            int lost = report.trackKind.equals(QNTrackKind.VIDEO) ? report.videoPacketLostRate : report.audioPacketLostRate;
            Log.i(TAG, "remote user " + report.userId
                + " rtt " + report.rtt
                + " grade " + report.networkGrade
                + " track " + report.trackId
                + " kind " + (report.trackKind.name())
                + " lostRate " + lost);
        }
    }

    @Override
    public void onAudioRouteChanged(QNAudioDevice routing) {

    }

    @Override
    public void onCreateMergeJobSuccess(String mergeJobId) {

    }

    @Override
    public void onMessageReceived(QNCustomMessage qnCustomMessage) {

    }

    /**
     * 当发生错误时会回调此方法
     *
     * @param errorCode 错误码，详情请参考 {@link QNErrorCode}
     * @param description 错误描述
     */
    @Override
    public void onError(int errorCode, String description) {
        /**
         * 关于错误异常的相关处理，都应在该回调中完成; 需要处理的错误码及建议处理逻辑如下:
         *
         *【TOKEN 相关】
         * 1. QNErrorCode.ERROR_TOKEN_INVALID 和 QNErrorCode.ERROR_TOKEN_ERROR 表示您提供的房间 token 不符合七牛 token 签算规则,
         *    详情请参考【服务端开发说明.RoomToken 签发服务】https://doc.qnsdk.com/rtn/docs/server_overview#1
         * 2. QNErrorCode.ERROR_TOKEN_EXPIRED 表示您的房间 token 过期, 需要重新生成 token 再加入；
         *
         *【房间设置相关】以下情况可以与您的业务服务开发确认具体设置
         * 1. QNErrorCode.ERROR_ROOM_FULL 当房间已加入人数超过每个房间的人数限制触发；请确认后台服务的设置；
         * 2. QNErrorCode.ERROR_PLAYER_ALREADY_EXIST 后台如果配置为开启【禁止自动踢人】,则同一用户重复加入/未正常退出再加入会触发此错误，您的业务可根据实际情况选择配置；
         * 3. QNErrorCode.ERROR_NO_PERMISSION 用户对于特定操作，如合流需要配置权限，禁止出现未授权的用户操作；
         * 4. QNErrorCode.ERROR_ROOM_CLOSED 房间已被管理员关闭；
         *
         *【其他错误】
         * 1. QNErrorCode.ERROR_AUTH_FAIL 服务验证时出错，可能为服务网络异常。建议重新尝试加入房间；
         * 2. QNErrorCode.ERROR_PUBLISH_FAIL 发布失败, 会有如下3种情况:
         * 1 ）请确认成功加入房间后，再执行发布操作
         * 2 ）请确定对于音频/视频 Track，分别最多只能有一路为 master
         * 3 ）请确认您的网络状况是否正常

         * 3. QNErrorCode.ERROR_RECONNECT_TOKEN_ERROR 内部重连后出错，一般出现在网络非常不稳定时出现，建议提示用户并尝试重新加入房间；
         * 4. QNErrorCode.ERROR_INVALID_PARAMETER 服务交互参数错误，请在开发时注意合流、踢人动作等参数的设置。
         * 5. QNErrorCode.ERROR_DEVICE_CAMERA 系统摄像头错误, 建议提醒用户检查
         */
        switch (errorCode) {
            // RoomToken 无效，建议重新获取 RoomToken 后再加入房间
            case QNErrorCode.ERROR_TOKEN_INVALID:
            // RoomToken 错误，建议收到此错误代码时尝试重新获取 RoomToken 后再次加入房间
            case QNErrorCode.ERROR_TOKEN_ERROR:
                Toast.makeText(getApplicationContext(), "RoomToken Error", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_TOKEN_EXPIRED:
                // RoomToken 过期，建议重新获取 RoomToken 后再加入房间
                Toast.makeText(getApplicationContext(), "RoomToken Expired", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_AUTH_FAIL:
                // RoomToken 鉴权失败，建议收到此错误代码时尝试再次加入房间。
                Toast.makeText(getApplicationContext(), "Auth Fail", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_PUBLISH_FAIL:
                // 发布失败，建议收到此错误代码时检查当前连接状态并尝试重新发布
                if (QNRoomState.CONNECTED.equals(mEngine.getRoomState()) || QNRoomState.RECONNECTED.equals(mEngine.getRoomState())) {
                    mEngine.publish();
                }
                break;
            case QNErrorCode.ERROR_ROOM_CLOSED:
                // 房间已被管理员关闭
                Toast.makeText(getApplicationContext(), "Room closed by admin", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_ROOM_FULL:
                // 房间已超过限制，可在在七牛管理控制台中设置房间内最大人数
                Toast.makeText(getApplicationContext(), "Room is full", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_PLAYER_ALREADY_EXIST:
                // 用户已存在，可能是同一用户在其他设备加入了房间，可在七牛管理控制台中设置是否允许同一个身份用户重复加入房间
                Toast.makeText(getApplicationContext(), "Already login on other device", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_NO_PERMISSION:
                // 权限不足，一般在踢人、合流等需要权限的操作中会出现
                Toast.makeText(getApplicationContext(), "You can not do this operation", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_RECONNECT_TOKEN_ERROR:
                // SDK 重连时间过长，内部的重连 Token 已失效，建议重新加入房间
                Toast.makeText(getApplicationContext(), "RoomToken Error", Toast.LENGTH_SHORT).show();
                mEngine.joinRoom(mRoomToken);
                break;
            case QNErrorCode.ERROR_INVALID_PARAMETER:
                // 参数错误，一般在踢人、合流等操作中会出现，开发者检查业务代码逻辑
                Toast.makeText(getApplicationContext(), "Parameters error", Toast.LENGTH_SHORT).show();
                break;
            case QNErrorCode.ERROR_DEVICE_CAMERA:
                // 相机错误：打开失败、没有权限或者被其他程序占用等
                Toast.makeText(getApplicationContext(), "Camera Error", Toast.LENGTH_SHORT).show();
                break;
            default:
                // 除上面的错误码外其他是 SDK 内部会进行处理的错误码，可以直接忽略
                Log.i(TAG, "errorCode = " + errorCode + " description = " + description);
        }
    }

    public void clickMuteVideo(View view) {
        ImageButton button = (ImageButton) view;
        mIsMuteVideo = !mIsMuteVideo;
        button.setImageDrawable(mIsMuteVideo ? getResources().getDrawable(R.mipmap.video_close) : getResources().getDrawable(R.mipmap.video_open));

        // mute 本地视频
        mEngine.muteLocalVideo(mIsMuteVideo);
    }

    public void clickMuteAudio(View view) {
        ImageButton button = (ImageButton) view;
        mIsMuteAudio = !mIsMuteAudio;
        button.setImageDrawable(mIsMuteAudio ? getResources().getDrawable(R.mipmap.microphone_disable) : getResources().getDrawable(R.mipmap.microphone));

        // mute 本地音频
        mEngine.muteLocalAudio(mIsMuteAudio);
    }

    public void clickSwitchCamera(View view) {
        final ImageButton button = (ImageButton) view;

        // 切换摄像头
        mEngine.switchCamera(new QNCameraSwitchResultCallback() {
            @Override
            public void onCameraSwitchDone(final boolean isFrontCamera) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setImageDrawable(isFrontCamera ? getResources().getDrawable(R.mipmap.camera_switch_front) : getResources().getDrawable(R.mipmap.camera_switch_end));
                    }
                });
            }

            @Override
            public void onCameraSwitchError(String errorMessage) {

            }
        });
    }

    public void clickHangUp(View view) {
        // 离开房间
        mEngine.leaveRoom();
        // 释放资源
        mEngine.destroy();
        finish();
    }
}
