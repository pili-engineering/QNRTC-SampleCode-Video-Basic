package com.qiniu.droid.rtc.sample1v1;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.qiniu.droid.rtc.QNAudioMixer;
import com.qiniu.droid.rtc.QNAudioMixerListener;
import com.qiniu.droid.rtc.QNAudioMixerState;
import com.qiniu.droid.rtc.QNCameraFacing;
import com.qiniu.droid.rtc.QNCameraSwitchResultCallback;
import com.qiniu.droid.rtc.QNCameraVideoTrack;
import com.qiniu.droid.rtc.QNCameraVideoTrackConfig;
import com.qiniu.droid.rtc.QNClientEventListener;
import com.qiniu.droid.rtc.QNConnectionState;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNConnectionDisconnectedInfo;
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
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNTrack;
import com.qiniu.droid.rtc.QNTrackInfoChangedListener;
import com.qiniu.droid.rtc.QNTrackProfile;
import com.qiniu.droid.rtc.QNVideoCaptureConfigPreset;
import com.qiniu.droid.rtc.QNVideoEncoderConfig;
import com.qiniu.droid.rtc.model.QNAudioDevice;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RoomActivity extends AppCompatActivity implements QNRTCEventListener, QNClientEventListener {
    private static final String TAG = "RoomActivity";
    private static final String TAG_CAMERA = "camera";
    private static final String TAG_MICROPHONE = "microphone";

    private QNSurfaceView mLocalVideoSurfaceView;
    private QNSurfaceView mRemoteVideoSurfaceView;
    private QNRTCClient mClient;
    private String mRoomToken;

    private QNCameraVideoTrack mCameraVideoTrack;
    private QNMicrophoneAudioTrack mMicrophoneAudioTrack;

    private boolean mIsMuteVideo = false;
    private boolean mIsMuteAudio = false;

    private Timer mStatsTimer;

    private FilePickerDialog mFilePickerDialog;
    private String mMusicFilePath;
    private QNAudioMixer mAudioMixer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        mLocalVideoSurfaceView = findViewById(R.id.local_video_surface_view);
        mRemoteVideoSurfaceView = findViewById(R.id.remote_video_surface_view);
        mRemoteVideoSurfaceView.setZOrderOnTop(true);// QNSurfaceView 是 SurfaceView 的子类, 会受层级影响

        Intent intent = getIntent();
        mRoomToken = intent.getStringExtra("roomToken");

        QNRTCSetting setting = new QNRTCSetting();

        // 设置 QNRTC Log 输出等级
        setting.setLogLevel(QNLogLevel.INFO);

        // 初始化 QNRTC
        QNRTC.init(getApplicationContext(), setting, this);

        // 创建本地 Camera 采集 track
        if (mCameraVideoTrack == null) {
            QNCameraVideoTrackConfig cameraVideoTrackConfig = new QNCameraVideoTrackConfig(TAG_CAMERA)
                    .setCameraFacing(QNCameraFacing.FRONT)
                    .setMultiProfileEnabled(true)
                    .setVideoCaptureConfig(QNVideoCaptureConfigPreset.CAPTURE_640x480)
                    .setVideoEncoderConfig(new QNVideoEncoderConfig(640, 480, 24, 800));

            mCameraVideoTrack = QNRTC.createCameraVideoTrack(cameraVideoTrackConfig);
        }
        // 设置预览窗口
        mCameraVideoTrack.play(mLocalVideoSurfaceView);

        // 创建本地音频采集 track
        if (mMicrophoneAudioTrack == null) {
            QNMicrophoneAudioTrackConfig microphoneAudioTrackConfig = new QNMicrophoneAudioTrackConfig(TAG_MICROPHONE);
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

    /**
     * 房间状态改变时会回调此方法
     * 房间状态回调只需要做提示用户，或者更新相关 UI； 不需要再做加入房间或者重新发布等其他操作！
     *
     * @param state 房间状态，可参考 {@link QNConnectionState}
     */
    @Override
    public void onConnectionStateChanged(QNConnectionState state, @Nullable QNConnectionDisconnectedInfo info) {
        switch (state) {
            case DISCONNECTED:
                // 初始化状态
                Log.i(TAG, "DISCONNECTED : " + info.getReason() + " , errorCode : " + info.getErrorCode() + " , errorMsg : " + info.getErrorMessage());
                break;
            case CONNECTING:
                // 正在连接
                Log.i(TAG, "CONNECTING");
                break;
            case CONNECTED:
                // 连接成功，即加入房间成功
                Log.i(TAG, "CONNECTED");
                // 保证房间内只有2人
                if (mClient.getRemoteUsers().size() > 1) {
                    Toast.makeText(RoomActivity.this, "You can't enter the room.", Toast.LENGTH_SHORT).show();
                    QNRTC.deinit();
                    finish();
                }

                // 加入房间成功后发布音视频数据，发布成功会触发 QNPublishResultCallback#onPublished 回调
                mClient.publish(new QNPublishResultCallback() {
                    @Override
                    public void onPublished() {
                        Log.i(TAG, "onPublished");
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) {
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
        Log.i(TAG, "onUserUnpublished : " + remoteUserId);
        for (QNTrack track : trackList) {
            if (TAG_CAMERA.equals(track.getTag())) {
                // 当远端视频取消发布时隐藏远端窗口
                mRemoteVideoSurfaceView.setVisibility(View.GONE);
            }
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
        for (QNRemoteVideoTrack track : remoteVideoTracks) {
            // 设置视频 track 渲染窗口
            track.play(mRemoteVideoSurfaceView);
            mRemoteVideoSurfaceView.setVisibility(View.VISIBLE);
            track.setProfile(QNTrackProfile.HIGH);
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

    }

    @Override
    public void onMediaRelayStateChanged(String relayRoom, QNMediaRelayState state) {

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
            public void onSwitched(final boolean isFrontCamera) {
                runOnUiThread(() -> button.setImageDrawable(isFrontCamera ? getResources().getDrawable(R.mipmap.camera_switch_front) : getResources().getDrawable(R.mipmap.camera_switch_end)));
            }

            @Override
            public void onError(String errorMessage) {
            }
        });
    }

    public void clickHangUp(View view) {
        // 离开房间
        mClient.leave();
        finish();
    }

    public void onClickChooseFile(View view) {
        if (mFilePickerDialog == null) {
            DialogProperties properties = new DialogProperties();
            properties.selection_mode = DialogConfigs.SINGLE_MODE;
            properties.selection_type = DialogConfigs.FILE_SELECT;
            properties.root = new File(DialogConfigs.DEFAULT_DIR);
            properties.extensions = new String[]{"mp3"};
            mFilePickerDialog = new FilePickerDialog(RoomActivity.this, properties);
            mFilePickerDialog.setDialogSelectionListener(new DialogSelectionListener() {
                @Override
                public void onSelectedFilePaths(String[] files) {
                    mMusicFilePath = files[0];
                }
            });
        }

        mFilePickerDialog.show();
    }

    public void onClickStartAudioMix(View view) {
        if (mMusicFilePath == null) {
            Toast.makeText(this, "请选择音乐文件！", Toast.LENGTH_SHORT).show();
            return;
        }
        mAudioMixer = mMicrophoneAudioTrack.createAudioMixer(mMusicFilePath, new QNAudioMixerListener() {
            @Override
            public void onStateChanged(QNAudioMixerState state) {
                Log.i(TAG, "audio mix onStateChanged : " + state.name());
            }

            @Override
            public void onMixing(long currentTimeUs) {
                Log.i(TAG, "audio mix onMixing : " + currentTimeUs / 1000);
            }

            @Override
            public void onError(int errorCode) {
                Log.i(TAG, "audio mix onError : " + errorCode);
            }
        });
        mAudioMixer.start();
    }

    public void onClickStopAudioMix(View view) {
        if (mAudioMixer == null) {
            Toast.makeText(this, "当前没有混音任务！", Toast.LENGTH_SHORT).show();
            return;
        }
        // 停止混音
        mAudioMixer.stop();
        mAudioMixer = null;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
