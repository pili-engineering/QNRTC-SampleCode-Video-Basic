package com.qiniu.droid.rtc.sample;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNSurfaceView;

public class CustomVideoView extends FrameLayout {
    private QNSurfaceView mVideoSurfaceView;
    private TextView mUserIdTextView;

    public CustomVideoView(Context context) {
        super(context);
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        View view = LayoutInflater.from(context).inflate(R.layout.custom_video_view, this, true);
        FrameLayout layout = findViewById(R.id.frame_layout);
        layout.getLayoutParams().width = outMetrics.widthPixels / 2;
        mVideoSurfaceView = view.findViewById(R.id.video_view);
        mUserIdTextView = view.findViewById(R.id.user_id_text_view);
    }

    public QNSurfaceView getVideoSurfaceView() {
        return mVideoSurfaceView;
    }

    public void setUserId(String userId) {
        mUserIdTextView.setText(userId);
    }
}
