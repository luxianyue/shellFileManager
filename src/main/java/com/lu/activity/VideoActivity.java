package com.lu.activity;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.percent.PercentRelativeLayout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.lu.filemanager2.R;
import com.lu.view.DialogManager;

import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bulefin on 2017/12/5.
 */

public class VideoActivity extends BasedActivity implements View.OnClickListener, View.OnTouchListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener,MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnErrorListener {
    private String mVideoPath;
    private VideoView mVideoView;
    private View mVideoTopBar;
    private View mVideoBottomBar;
    private View mVideoCenterImg;
    private ImageButton mPlayAndPauseBtn;
    private SeekBar mSeekBar;
    private TextView mTextViewCurrentTime;
    private TextView mTextViewTotalTime;

    private ScheduledExecutorService scheduledExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        PercentRelativeLayout prl = (PercentRelativeLayout) getLayoutInflater().inflate(R.layout.activity_videoview, null);
        setContentView(prl);

        prl.setOnTouchListener(this);
        mVideoView = (VideoView) prl.getChildAt(0);
        mVideoTopBar = prl.getChildAt(1);
        mVideoBottomBar = prl.getChildAt(2);
        mVideoCenterImg = prl.getChildAt(3);
        mVideoCenterImg.setOnClickListener(this);
        ((LinearLayout)mVideoTopBar).getChildAt(0).setOnClickListener(this);

        mPlayAndPauseBtn = (ImageButton) ((LinearLayout)mVideoBottomBar).getChildAt(0);
        mPlayAndPauseBtn.setOnClickListener(this);
        mSeekBar = (SeekBar) ((LinearLayout)mVideoBottomBar).getChildAt(2);
        mSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mTextViewCurrentTime = (TextView) ((LinearLayout)mVideoBottomBar).getChildAt(1);
        mTextViewTotalTime = (TextView) ((LinearLayout)mVideoBottomBar).getChildAt(3);
        mVideoPath = getIntent().getStringExtra("path");

        ((TextView)((LinearLayout) mVideoTopBar).getChildAt(1)).setText(mVideoPath.substring(mVideoPath.lastIndexOf("/") + 1));

        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnErrorListener(this);

        mSdf = new SimpleDateFormat("mm:ss");
        mSdf2 = new SimpleDateFormat("HH:mm:ss");
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp.getVideoWidth() > mp.getVideoHeight() && getResources().getDisplayMetrics().widthPixels < getResources().getDisplayMetrics().heightPixels) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        mPlayAndPauseBtn.setImageResource(R.drawable.ic_button_pause);
        mSeekBar.setMax(mp.getDuration());
        mTextViewTotalTime.setText(getStrTime(mp.getDuration()));
        scheduledExecutorService.scheduleAtFixedRate(mRunnable, 0, 1000, TimeUnit.MILLISECONDS);
        mp.setOnSeekCompleteListener(VideoActivity.this);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mPlayAndPauseBtn.setImageResource(R.drawable.ic_button_play);
        updatePlayProgress();
        mVideoCenterImg.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        startPlay();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN || what == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
            Object obj[] = DialogManager.get().getMsgDialog(this, this);
            mSeekBar.setTag(obj[0]);
            ((TextView)obj[1]).setText(R.string.audio_play_error);
            ((AlertDialog)obj[0]).show();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_play:
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    mPlayAndPauseBtn.setImageResource(R.drawable.ic_button_play);
                    mVideoCenterImg.setVisibility(View.VISIBLE);
                } else {
                    if (mVideoView.getCurrentPosition() == mVideoView.getDuration()) {
                        mVideoView.seekTo(0);
                    }
                    startPlay();
                }
                break;
            case R.id.video_center_img:
                v.setVisibility(View.GONE);
                if (mVideoView.getCurrentPosition() == mVideoView.getDuration()) {
                    mVideoView.seekTo(0);
                }
                startPlay();
                break;
            case R.id.msg_confirm:
                ((AlertDialog)mSeekBar.getTag()).dismiss();
            case R.id.video_back:
                finish();
                break;
        }
    }

    private void startPlay() {
        mVideoView.start();
        mPlayAndPauseBtn.setImageResource(R.drawable.ic_button_pause);
        if (mVideoCenterImg.getVisibility() == View.VISIBLE) {
            mVideoCenterImg.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mVideoTopBar.getVisibility() == View.GONE) {
            mVideoTopBar.setVisibility(View.VISIBLE);
            mVideoBottomBar.setVisibility(View.VISIBLE);
        }
        mHandler.removeCallbacks(mRunnableDismiss);
        mHandler.postDelayed(mRunnableDismiss, 3600);
        return false;
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mVideoView.isPlaying()) {
                mHandler.sendEmptyMessage(0);
            }
        }
    };

    private Runnable mRunnableDismiss = new Runnable() {
        @Override
        public void run() {
            mVideoTopBar.setVisibility(View.GONE);
            mVideoBottomBar.setVisibility(View.GONE);
        }
    };

    private void updatePlayProgress() {
        int progress = mVideoView.getCurrentPosition();
        //System.out.println(mVideoView.getBufferPercentage() + "--->" + progress);
        mTextViewCurrentTime.setText(getStrTime(progress));
        mSeekBar.setProgress(progress);
        if (mSeekBar.getSecondaryProgress() != mVideoView.getDuration()) {
            mSeekBar.setSecondaryProgress(mVideoView.getDuration() * mVideoView.getBufferPercentage() / 100);
        }
    }

    @Override
    protected void onDestroy() {
        mVideoView.stopPlayback();
        scheduledExecutorService.shutdownNow();
        super.onDestroy();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            updatePlayProgress();
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mVideoView.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private SimpleDateFormat mSdf, mSdf2;
    private String getStrTime(int totalTime) {
        if (totalTime < 3600000) {
            return mSdf.format(totalTime);
        }
        return mSdf2.format(totalTime);
    }

}
