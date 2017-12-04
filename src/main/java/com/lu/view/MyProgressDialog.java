package com.lu.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.lu.App;
import com.lu.activity.TextActivity;
import com.lu.filemanager2.MainActivity;
import com.lu.filemanager2.R;
import com.lu.utils.FileUtils;

/**
 * Created by bulefin on 2017/11/30.
 */

public class MyProgressDialog extends AlertDialog implements View.OnClickListener{
    private AlertDialog tipDialog;
    private ImageView mProgressImg;
    private Animation mAnimation;
    private static int position;
    private static MyProgressDialog mDialog, mDialog2;
    public MyProgressDialog(@NonNull Context context) {
        super(context, R.style.MyProgressDialog);
        setCancelable(false);
    }

    public static void show(Activity activity) {
        if (activity instanceof TextActivity) {
            if (mDialog2 == null) {
                mDialog2 = new MyProgressDialog(activity);
            }
            mDialog2.show();
            position = 2;
        }
        if (activity instanceof MainActivity) {
            if (mDialog == null) {
                mDialog = new MyProgressDialog(activity);
            }
            mDialog.show();
            position = 1;
        }
    }

    public static void close() {
        if (position == 1) {
            if (mDialog == null) {
                return;
            }
            if (mDialog.isShowing())
                mDialog.dismiss();
        }
        if (position == 2) {
            if (mDialog2 == null) {
                return;
            }
            if (mDialog2.isShowing())
                mDialog2.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myprogress_dialog_layout);
        setCanceledOnTouchOutside(false);
        mProgressImg = (ImageView) findViewById(R.id.myprogress_dialog_img);
        mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.myprogress_rotate);
        //动画完成后，是否保留动画最后的状态，设为true
        mAnimation.setFillAfter(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAnimation != null) {
            mProgressImg.startAnimation(mAnimation);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mProgressImg.clearAnimation();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (position == 1) {

        }
        if (position == 2) {
            if (tipDialog == null) {

            }
            dismiss();
            FileUtils.isStop = true;
        }
    }

    public static void clearDialog(int position) {
        if (position == 1) {
            mDialog = null;
        }
        if (position == 2) {
            mDialog2 = null;
        }
    }

    @Override
    public void onClick(View v) {

    }
}
