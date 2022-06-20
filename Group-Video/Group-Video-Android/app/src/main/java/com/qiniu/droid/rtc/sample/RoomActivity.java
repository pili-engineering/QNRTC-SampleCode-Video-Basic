package com.qiniu.droid.rtc.sample;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;

import com.qiniu.droid.rtc.QNAudioQualityPreset;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraSwitchResultCallback;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNLocalAudioTrackStats;
import com.qiniu.droid.rtc.QNLocalVideoTrackStats;
import com.qiniu.droid.rtc.QNLogLevel;
import com.qiniu.droid.rtc.QNMediaRelayState;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrack;
import com.qiniu.droid.rtc.QNMicrophoneAudioTrackConfig;
import com.qiniu.droid.rtc.QNNetworkQuality;
import com.qiniu.droid.rtc.QNNetworkQualityListener;
import com.qiniu.droid.rtc.QNPublishResultCallback;
import com.qiniu.droid.rtc.QNRTC;
import com.qiniu.droid.rtc.QNRTCClient;
import com.qiniu.droid.rtc.QNRTCEventListener;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteAudioTrack;
import com.qiniu.droid.rtc.QNRemoteAudioTrackStats;
import com.qiniu.droid.rtc.QNRemoteTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrack;
import com.qiniu.droid.rtc.QNRemoteVideoTrackStats;
import com.qiniu.droid.rtc.QNTrackInfoChangedListener;
import com.qiniu.droid.rtc.QNTrackProfile;
import com.qiniu.droid.rtc.QNVideoCaptureConfigPreset;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RoomActivity extends AppCompatActivity implements QNRTCEventListener, QNClientEventListener {
    private static final String TAG = "RoomActivity";
    private static final String TAG_CAMERA = "camera";
    private static final String TAG_MICROPHONE = "microphone";

    private GridLayout mVideoSurfaceViewGroup;
    private CustomVideoView mLocalVideoView;
    private QNRTCClient mClient;
    private Map<String, CustomVideoView> mVideoViewMap = new HashMap<>();
    private String mRoomToken;

    private QNCameraVideoTrack mCameraVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;

    private boolean mIsMuteVideo = false;
    private boolean mIsMuteAudio = false;

    private Timer mStatsTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        mVideoSurfaceViewGroup = findViewById(R.id.video_surface_view_group);

        Intent intent = getIntent();
        mRoomToken = intent.getStringExtra("roomToken");

        QNRTCSetting setting = new QNRTCSetting();

        // 设置 QNRTC Log 输出等级
        setting.setLogLevel(QNLogLevel.INFO);

        // 配置音视频数据的编码方式，此处可配置为硬编
        setting.setHWCodecEnabled(false);

        // 初始化 QNRTC
        QNRTC.init(getApplicationContext(), setting, this);

        // 创建本地 Camera 采集 track
        if (mCameraVideoTrack == null) {
            QNCameraVideoTrackConfig cameraVideoTrackConfig = new QNCameraVideoTrackConfig(TAG_CAMERA)
                    .setCameraFacing(QNCameraFacing.FRONT)
                    .setVideoCaptureConfig(QNVideoCaptureConfigPreset.CAPTURE_640x480)
                    .setVideoEncoderConfig(new QNVideoEncoderConfig(640, 480, 24, 800));

            mCameraVideoTrack = QNRTC.createCameraVideoTrack(cameraVideoTrackConfig);
        }
        // 设置预览窗口
        mLocalVideoView = new CustomVideoView(this);
        mVideoSurfaceViewGroup.addView(mLocalVideoView);
        mCameraVideoTrack.play(mLocalVideoView.getVideoSurfaceView());

        // 创建本地音频采集 track
        if (mMicrophoneAudioTrack == null) {
            QNMicrophoneAudioTrackConfig microphoneAudioTrackConfig = new QNMicrophoneAudioTrackConfig(TAG_MICROPHONE);
            microphoneAudioTrackConfig.setAudioQuality(QNAudioQualityPreset.STANDARD);
            mMicrophoneAudioTrack = QNRTC.createMicrophoneAudioTrack(microphoneAudioTrackConfig);
        }

        // 创建 QNRTCClient
        mClient = QNRTC.createClient(this);
        mClient.join(mRoomToken);

        mClient.setNetworkQualityListener(new QNNetworkQualityListener() {
            @Override
            public void onNetworkQualityNotified(QNNetworkQuality qnNetworkQuality) {
                Log.i(TAG, "local: network quality: " + qnNetworkQuality.toString());
            }
        });
        mStatsTimer = new Timer();
        mStatsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // local video track
                Map<String, List<QNLocalVideoTrackStats>> localVideoTrackStats = mClient.getLocalVideoTrackStats();
                for (Map.Entry<String, List<QNLocalVideoTrackStats>> entry : localVideoTrackStats.entrySet()) {
                    for (QNLocalVideoTrackStats stats : entry.getValue()) {
                        Log.i(TAG, "local: trackID : " + entry.getKey() + ", " + stats.toString());
                    }
                }
                // local audio track
                Map<String, QNLocalAudioTrackStats> localAudioTrackStats = mClient.getLocalAudioTrackStats();
                for (Map.Entry<String, QNLocalAudioTrackStats> entry : localAudioTrackStats.entrySet()) {
                    Log.i(TAG, "local: trackID : " + entry.getKey() + ", " + entry.getValue().toString());
                }
                // remote video track
                Map<String, QNRemoteVideoTrackStats> remoteVideoTrackStats = mClient.getRemoteVideoTrackStats();
                for (Map.Entry<String, QNRemoteVideoTrackStats> entry : remoteVideoTrackStats.entrySet()) {
                    Log.i(TAG, "remote: trackID : " + entry.getKey() + ", " + entry.getValue().toString());
                }
                // remote audio track
                Map<String, QNRemoteAudioTrackStats> remoteAudioTrackStats = mClient.getRemoteAudioTrackStats();
                for (Map.Entry<String, QNRemoteAudioTrackStats> entry : remoteAudioTrackStats.entrySet()) {
                    Log.i(TAG, "remote: trackID : " + entry.getKey() + ", " + entry.getValue().toString());
                }
                // network
                Map<String, QNNetworkQuality> userNetworkQuality = mClient.getUserNetworkQuality();
                for (Map.Entry<String, QNNetworkQuality> entry : userNetworkQuality.entrySet()) {
                    Log.i(TAG, "remote: network quality: userID : " + entry.getKey() + ", " + entry.getValue().toString());
                }
            }
        }, 0, 10000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 需要及时反初始化 QNRTC 以释放资源
        QNRTC.deinit();
        mStatsTimer.cancel();
    }

    public void clickMuteVideo(View view) {
        ImageButton button = (ImageButton) view;
        mIsMuteVideo = !mIsMuteVideo;
        button.setImageDrawable(mIsMuteVideo ? getResources().getDrawable(R.mipmap.video_close) : getResources().getDrawable(R.mipmap.video_open));

        // mute 本地视频
        mCameraVideoTrack.setMuted(mIsMuteVideo);
    }

    public void clickMuteAudio(View view) {
        ImageButton button = (ImageButton) view;
        mIsMuteAudio = !mIsMuteAudio;
        button.setImageDrawable(mIsMuteAudio ? getResources().getDrawable(R.mipmap.microphone_disable) : getResources().getDrawable(R.mipmap.microphone));

        // mute 本地音频
        mMicrophoneAudioTrack.setMuted(mIsMuteAudio);
    }

    public void clickSwitchCamera(View view) {
        final ImageButton button = (ImageButton) view;

        // 切换摄像头
        mCameraVideoTrack.switchCamera(new QNCameraSwitchResultCallback() {
            @Override
            public void onSwitched(boolean isFrontCamera) {
                runOnUiThread(() -> button.setImageDrawable(isFrontCamera ? getResources().getDrawable(R.mipmap.camera_switch_front) : getResources().getDrawable(R.mipmap.camera_switch_end)));
            }

            @Override
            public void onError(String s) {

            }
        });
    }

    public void clickHangUp(View view) {
        // 离开房间
        mClient.leave();
        finish();
    }

    /**
     * 房间连接状态改变时会回调此方法
     *
     * @param state 当前的房间连接状态
     * @param info 房间连接状态断开时，表示断开的原因
     */
    @Override
    public void onConnectionStateChanged(QNConnectionState state, @Nullable QNConnectionDisconnectedInfo info) {
        switch (state) {
            case DISCONNECTED:
                // 初始化以及房间连接断开的状态。
                // 房间连接断开场景下的信息获取方式可参考 https://developer.qiniu.com/rtc/8644/QNConnectionDisconnectedInfo
                Log.i(TAG, "DISCONNECTED : " + info.getReason() + " , errorCode : " + info.getErrorCode() + " , errorMsg : " + info.getErrorMessage());
                break;
            case CONNECTING:
                // 正在连接
                Log.i(TAG, "CONNECTING");
                break;
            case CONNECTED:
                // 连接成功，即加入房间成功
                Log.i(TAG, "CONNECTED");

                // 加入房间成功后发布音视频数据，发布成功会触发 QNPublishResultCallback#onPublished 回调
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() {
                        Log.i(TAG, "onPublished");
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) {
                        // 可能的错误码请参考：https://developer.qiniu.com/rtc/8673/QNPublishResultCallback
                        Log.i(TAG, "publish failed : " + errorCode + " " + errorMessage);
                    }
                }, mCameraVideoTrack, mMicrophoneAudioTrack);
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

    /**
     * 远端用户加入房间时会回调此方法
     *
     * @param remoteUserId 远端用户的 userId
     * @param userData     透传字段，用户自定义内容
     * @see QNRTCClient#join(String, String) 可指定 userData 字段
     */
    @Override
    public void onUserJoined(String remoteUserId, String userData) {
        Log.i(TAG, "onUserJoined : " + remoteUserId);
    }

    /**
     * 远端用户开始重连时会回调此方法
     *
     * @param remoteUserId 远端用户 ID
     */
    @Override
    public void onUserReconnecting(String remoteUserId) {
        Log.i(TAG, "onUserReconnecting : " + remoteUserId);
    }

    /**
     * 远端用户重连成功时会回调此方法
     *
     * @param remoteUserId 远端用户 ID
     */
    @Override
    public void onUserReconnected(String remoteUserId) {
        Log.i(TAG, "onUserReconnected : " + remoteUserId);
    }

    /**
     * 远端用户离开房间时会回调此方法
     *
     * @param remoteUserId 远端离开用户的 userId
     */
    @Override
    public void onUserLeft(String remoteUserId) {
        Log.i(TAG, "onUserLeft : " + remoteUserId);
    }

    /**
     * 远端用户 tracks 成功发布时会回调此方法
     *
     * @param remoteUserId 远端用户 userId
     * @param trackList    远端用户发布的 tracks 列表
     */
    @Override
    public void onUserPublished(String remoteUserId, List<QNRemoteTrack> trackList) {
        Log.i(TAG, "onUserPublished : " + remoteUserId);
    }

    /**
     * 远端用户 tracks 成功取消发布时会回调此方法
     *
     * @param remoteUserId 远端用户 userId
     * @param trackList    远端用户取消发布的 tracks 列表
     */
    @Override
    public void onUserUnpublished(String remoteUserId, List<QNRemoteTrack> trackList) {
        // 当远端视频取消发布时删除远端窗口
        CustomVideoView remoteVideoView = mVideoViewMap.get(remoteUserId);
        if (remoteVideoView != null) {
            mVideoSurfaceViewGroup.removeView(remoteVideoView);
            mVideoViewMap.remove(remoteUserId);
        }
    }

    /**
     * 成功订阅远端用户的 tracks 时会回调此方法
     *
     * @param remoteUserID 远端用户 userID
     * @param remoteAudioTracks 订阅的远端用户音频 tracks 列表
     * @param remoteVideoTracks 订阅的远端用户视频 tracks 列表
     */
    @Override
    public void onSubscribed(String remoteUserID, List<QNRemoteAudioTrack> remoteAudioTracks, List<QNRemoteVideoTrack> remoteVideoTracks) {
        if (mVideoViewMap.get(remoteUserID) != null) {
            // 处理掉重连后的订阅渲染到窗口
            return;
        }
        // 筛选出视频 Track 以渲染到窗口
        for (QNRemoteVideoTrack track : remoteVideoTracks) {
            // 设置视频 track 渲染窗口
            CustomVideoView remoteVideoView = new CustomVideoView(this);
            mVideoSurfaceViewGroup.addView(remoteVideoView);
            track.play(remoteVideoView.getVideoSurfaceView());
            remoteVideoView.setUserId(remoteUserID);
            mVideoViewMap.put(remoteUserID, remoteVideoView);
            // 设置视频 track 信息改变监听器
            track.setTrackInfoChangedListener(new QNTrackInfoChangedListener() {
                @Override
                public void onVideoProfileChanged(QNTrackProfile profile) {
                    // 订阅的视频 track Profile 改变。
                    // Profile 详情可参考 https://developer.qiniu.com/rtc/8772/video-size-flow-android
                }

                @Override
                public void onMuteStateChanged(boolean isMuted) {
                    // 远端视频 track 静默状态改变
                    Log.i(TAG, "远端视频 track : " + track.getTrackID() + " mute : " + isMuted);
                }
            });
        }
        // 设置音频 track 信息改变监听器
        for (QNRemoteAudioTrack track : remoteAudioTracks) {
            track.setTrackInfoChangedListener(new QNTrackInfoChangedListener() {
                @Override
                public void onMuteStateChanged(boolean isMuted) {
                    // 远端音频 track 静默状态改变
                    Log.i(TAG, "远端音频 track : " + track.getTrackID() + " mute : " + isMuted);
                }
            });
        }
    }

    /**
     * 当收到自定义消息时回调此方法
     *
     * @param message 自定义信息，详情请参考 {@link QNCustomMessage}
     */
    @Override
    public void onMessageReceived(QNCustomMessage message) {
        Log.i(TAG, "onMessageReceived : " + message.getContent());
    }

    @Override
    public void onMediaRelayStateChanged(String s, QNMediaRelayState qnMediaRelayState) {

    }

    /**
     * 当音频路由发生变化时会回调此方法
     *
     * @param device 音频设备, 详情请参考{@link QNAudioDevice}
     */
    @Override
    public void onAudioRouteChanged(QNAudioDevice device) {
        Log.i(TAG, "onAudioRouteChanged : " + device.name());
    }
}
