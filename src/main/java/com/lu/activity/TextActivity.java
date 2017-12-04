package com.lu.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.percent.PercentRelativeLayout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lu.App;
import com.lu.filemanager2.R;
import com.lu.utils.FileUtils;
import com.lu.utils.ShellUtil;
import com.lu.utils.ToastUitls;
import com.lu.view.MyProgressDialog;

import java.lang.ref.WeakReference;

/**
 * Created by bulefin on 2017/11/28.
 */

public class TextActivity extends BasedActivity implements View.OnClickListener, ShellUtil.onTextActivityListener {
    EditText editText;
    TextView textViewName;
    TextView tvLookMode, tvEditMode;
    PopupWindow popupWindow;
    MyHandler myHandler;
    ImageView animationImg;

    String mPath;
    int textFlag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PercentRelativeLayout ll = (PercentRelativeLayout) getLayoutInflater().inflate(R.layout.activity_text, null);
        setContentView(ll);
        myHandler = new MyHandler(this);
        editText = (EditText) ((ScrollView)ll.getChildAt(1)).getChildAt(0);
        animationImg = (ImageView) ll.getChildAt(2);
        animationImg.setAnimation(AnimationUtils.loadAnimation(this, R.anim.myprogress_rotate));
        animationImg.setTag(animationImg.getAnimation());
        textViewName = (TextView) ((HorizontalScrollView) ((LinearLayout)ll.getChildAt(0)).getChildAt(1)).getChildAt(0);
        mPath = getIntent().getStringExtra("path");
        textViewName.setText(mPath.substring(mPath.lastIndexOf("/") + 1));
        ((LinearLayout)ll.getChildAt(0)).getChildAt(2).setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        editText.getText().clear();
        showProgress();
        textFlag = 1;
        ShellUtil.get().setTextActivityListener(this);
        final WeakReference<MyHandler> handler = new WeakReference<MyHandler>(myHandler);
        new Thread() {
            @Override
            public void run() {
                FileUtils.lookTextContent(handler.get(), App.tempFilePath);
            }
        }.start();
    }

    private void showProgress(){
        animationImg.setVisibility(View.VISIBLE);
        animationImg.startAnimation((Animation) animationImg.getTag());
    }

    private void closeProgress(){
        animationImg.clearAnimation();
        animationImg.setVisibility(View.GONE);
    }

    @Override
    public void onTextSaveAction(String str) {
        textFlag = 0;
        closeProgress();
        ToastUitls.showSMsg("保存成功");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.text_imgbutton:
                if (textFlag == 1) {
                    ToastUitls.showSMsg("内容正在加载中，请稍等...");
                    return;
                }
                if (textFlag == 2) {
                    ToastUitls.showSMsg("内容正在保存中，请稍等...");
                    return;
                }
                if (popupWindow == null) {
                    LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.text_imagebutton_menu, null);
                    tvLookMode = (TextView) ll.getChildAt(2);
                    tvEditMode = (TextView) ll.getChildAt(4);
                    ll.getChildAt(0).setOnClickListener(this);
                    tvLookMode.setOnClickListener(this);
                    tvEditMode.setOnClickListener(this);
                    ll.getChildAt(6).setOnClickListener(this);
                    int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160f, getResources().getDisplayMetrics());
                    popupWindow = new PopupWindow(ll, width, -2, true);
                    popupWindow.setBackgroundDrawable(new ColorDrawable());
                    popupWindow.setOutsideTouchable(false);
                }
                int location[] = {0,0};
                v.getLocationOnScreen(location);
                popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, location[0] - popupWindow.getWidth() + v.getWidth() * 9 / 10, location[1] + textViewName.getHeight());
                break;
            case R.id.text_imagebutton_saveedit:
                popupWindow.dismiss();
                showProgress();
                textFlag = 2;
                new Thread() {
                    @Override
                    public void run() {
                        FileUtils.saveTextContent(myHandler, editText.getText().toString().getBytes());
                    }
                }.start();
                break;
            case R.id.text_imagebutton_lookmode:
                //设置不可编辑状态：
                editText.setFocusable(false);
                editText.setFocusableInTouchMode(false);
                popupWindow.dismiss();
                tvLookMode.setEnabled(false);
                tvLookMode.setTextColor(getResources().getColor(R.color.per_setfont_color_notable));
                tvEditMode.setEnabled(true);
                tvEditMode.setTextColor(getResources().getColor(R.color.white));
                break;
            case R.id.text_imagebutton_editmode:
                //设置可编辑状态：
                editText.setFocusableInTouchMode(true);
                editText.setFocusable(true);
                editText.requestFocus();
                popupWindow.dismiss();
                tvEditMode.setEnabled(false);
                tvEditMode.setTextColor(getResources().getColor(R.color.per_setfont_color_notable));
                tvLookMode.setEnabled(true);
                tvLookMode.setTextColor(getResources().getColor(R.color.white));
                break;
            case R.id.text_imagebutton_exit:
                popupWindow.dismiss();
                finish();
                break;
        }
    }

    static class MyHandler extends Handler {
        WeakReference<TextActivity> mainThis;
        public MyHandler(TextActivity ty) {
            mainThis = new WeakReference<TextActivity>(ty);
        }
        @Override
        public void handleMessage(Message msg) {
            TextActivity ty = mainThis.get();
            if (ty != null) {
                switch (msg.what) {
                    case -1:
                        ty.textFlag = 0;
                        ty.closeProgress();
                        break;
                    case 1:
                        ty.editText.append(msg.obj.toString());
                        break;
                    case 2:
                        FileUtils.get().do_text(ty.mPath, App.tempFilePath, 'e');
                        break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.isStop = true;
    }
}
