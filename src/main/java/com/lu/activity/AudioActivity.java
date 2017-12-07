package com.lu.activity;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lu.filemanager2.R;
import com.lu.view.DialogManager;

import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bulefin on 2017/12/5.
 */

public class AudioActivity extends BasedActivity implements View.OnClickListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnCompletionListener {
    private SeekBar mSeekBar;
    private TextView mTextViewCurrentTime;
    private TextView mTextViewTotalTime;
    private ImageButton mPlayAndPauseImg;
    private MediaPlayer mMediaPlayer;

    private ScheduledExecutorService scheduledExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_audio, null);
        setContentView(ll);
        final WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350f, getResources().getDisplayMetrics());
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, getResources().getDisplayMetrics());
        getWindow().setAttributes(params);
        mTextViewCurrentTime = (TextView) ll.getChildAt(0);
        mTextViewTotalTime = (TextView) ll.getChildAt(2);
        mSeekBar = (SeekBar) ll.getChildAt(1);
        mPlayAndPauseImg = (ImageButton) ll.getChildAt(3);
        mPlayAndPauseImg.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        mMediaPlayer = MediaPlayer.create(this, Uri.parse("file://" + getIntent().getStringExtra("path")));
        if (mMediaPlayer == null) {
            Object obj[] = DialogManager.get().getMsgDialog(this, this);
            mSeekBar.setTag(obj[0]);
            ((TextView)obj[1]).setText(R.string.audio_play_error);
            ((AlertDialog)obj[0]).show();
            return;
        }
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);

        mSdf = new SimpleDateFormat("mm:ss");
        mSdf2 = new SimpleDateFormat("HH:mm:ss");
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mSeekBar.setMax(mp.getDuration());
        mSeekBar.setSecondaryProgress(mp.getDuration());
        mTextViewTotalTime.setText(getStrTime(mp.getDuration()));
        scheduledExecutorService.scheduleAtFixedRate(mRunnable, 0, 1000, TimeUnit.MILLISECONDS);
        startPlay();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mPlayAndPauseImg.setImageResource(R.drawable.ic_button_play);
        updatePlayProgress();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        startPlay();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.msg_confirm) {
            ((AlertDialog)mSeekBar.getTag()).dismiss();
            finish();
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mPlayAndPauseImg.setImageResource(R.drawable.ic_button_play);
            } else {
                startPlay();
            }
        }
    }

    private void startPlay() {
        mMediaPlayer.start();
        mPlayAndPauseImg.setImageResource(R.drawable.ic_button_pause);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer.isPlaying()) {
                mHandler.sendEmptyMessage(0);
            }
        }
    };

    private void updatePlayProgress() {
        int progress = mMediaPlayer.getCurrentPosition();
        mTextViewCurrentTime.setText(getStrTime(progress));
        mSeekBar.setProgress(progress);
    }

    @Override
    protected void onDestroy() {
        System.out.println("ondestroy----->");
        if (mMediaPlayer != null) {
            scheduledExecutorService.shutdownNow();
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
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
                mMediaPlayer.seekTo(progress);
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
