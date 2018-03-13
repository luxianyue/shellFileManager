package com.lu.activity;

import android.app.AlertDialog;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.lu.App;
import com.lu.filemanager2.R;
import com.lu.model.TempItem;
import com.lu.utils.FileUtils;
import com.lu.utils.PermissionUtils;
import com.lu.utils.ShellUtils;
import com.lu.utils.ToastUitls;
import com.lu.view.DialogManager;

import java.lang.ref.WeakReference;

/**
 * Created by bulefin on 2017/11/28.
 */

public class TextActivity extends BasedActivity implements View.OnClickListener, FileUtils.OnCommonListener {

    private static int req_flag_mount = 1;
    private static int req_flag_save_edit = 2;
    private int req_flag;

    EditText editText;
    TextView textViewName;
    TextView tvSave, tvLookMode, tvEditMode;
    PopupWindow popupWindow;
    MyHandler myHandler;
    ImageView animationImg;
    AlertDialog mTipDialog;

    String mPath;
    int textFlag;
    int mIndex;
    String mStrs[];

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
        mIndex = getIntent().getIntExtra("index", -1);
        textViewName.setText(mPath.substring(mPath.lastIndexOf("/") + 1));
        ((LinearLayout)ll.getChildAt(0)).getChildAt(2).setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        editText.getText().clear();
        showProgress();
        textFlag = 1;
        FileUtils.get().addCommonListener(-1, this);
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
    public void onTextSaveAction(TempItem item) {
        textFlag = 0;
        closeProgress();
        if (item.error != null) {
            if (item.error.toLowerCase().contains("permission denied")) {
                req_flag = req_flag_save_edit;
                FileUtils.get().requestRoot(-1);
            }
            if (item.error.toLowerCase().contains("read-only file system")) {
                req_flag = req_flag_save_edit;
                FileUtils.get().requestRoot(-1);
            }
        } else {
            ToastUitls.showSMsg("保存成功");
        }
    }

    @Override
    public void onRealPath(String realPath) {
        FileUtils.get().checkMount(-1, realPath);
    }

    @Override
    public void onRequestRoot(boolean success) {
        if (success) {
            if (req_flag == req_flag_mount) {
                FileUtils.get().mountRW(mStrs[1], mStrs[2], mStrs[3], -1);
            }
            if (req_flag == req_flag_save_edit) {
                FileUtils.get().checkMount(-1, mPath);
                //saveText();
            }
        } else {
            if (req_flag == req_flag_save_edit) {
                ToastUitls.showSMsg("保存失败");
            }
        }
    }

    @Override
    public void onMountAction(boolean isCheck, boolean sucess) {
        if (isCheck) {
            mStrs = PermissionUtils.arrays;
            System.out.println("====================================================>"+mStrs[0] + "  " + mStrs[1] + "  " + mStrs[2]);
            if (req_flag == req_flag_mount) {
                if (Boolean.parseBoolean(mStrs[0])) {
                    if (mTipDialog == null) {
                        mTipDialog = DialogManager.get().createTiPDialog(this, this);
                    }
                    mTipDialog.show();
                    return;
                }
                setEditTextState(true);
            }
            if (req_flag == req_flag_save_edit) {
                if (Boolean.parseBoolean(mStrs[0])) {
                    FileUtils.get().mountRW(mStrs[1], mStrs[2], mStrs[3], -1);
                } else {
                    saveText();
                }
            }

        } else {
            if (sucess) {
                // mount success
                if (req_flag == req_flag_mount) {
                    setEditTextState(true);
                }
                if (req_flag == req_flag_save_edit) {
                    saveText();
                }
            }
        }
    }

    private void setEditTextState(boolean state) {
        editText.setFocusableInTouchMode(state);
        editText.setFocusable(state);
        editText.requestFocus();
        popupWindow.dismiss();
        tvSave.setEnabled(state);
        tvEditMode.setEnabled(!state);
        tvLookMode.setEnabled(state);
        if (state) {
            tvSave.setTextColor(getResources().getColor(R.color.white));
            tvLookMode.setTextColor(getResources().getColor(R.color.white));
            tvEditMode.setTextColor(getResources().getColor(R.color.per_setfont_color_notable));
        } else {
            tvSave.setTextColor(getResources().getColor(R.color.per_setfont_color_notable));
            tvLookMode.setTextColor(getResources().getColor(R.color.per_setfont_color_notable));
            tvEditMode.setTextColor(getResources().getColor(R.color.white));
        }
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
                    tvSave = (TextView) ll.getChildAt(0);
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
                saveText();
                break;
            case R.id.tip_confirm:
                mTipDialog.dismiss();
                if (mStrs != null) {
                    System.out.println("dev: "+ mStrs[1] + "<--->name: " + mStrs[2] + "<--->format: " + mStrs[3]);
                    if (ShellUtils.get().isRoot()) {
                        FileUtils.get().mountRW(mStrs[1], mStrs[2], mStrs[3], -1);
                    } else {
                        FileUtils.get().requestRoot(-1);
                    }
                }
                break;
            case R.id.tip_cancel:
                mTipDialog.dismiss();
                break;
            case R.id.text_imagebutton_lookmode:
                //设置不可编辑状态：
                setEditTextState(false);
                break;
            case R.id.text_imagebutton_editmode:
                //设置可编辑状态：
                req_flag = req_flag_mount;
                FileUtils.get().checkRealPath(-1, mPath);
                break;
            case R.id.text_imagebutton_exit:
                popupWindow.dismiss();
                finish();
                break;
        }
    }

    private void saveText() {
        showProgress();
        textFlag = 2;
        new Thread() {
            @Override
            public void run() {
                FileUtils.saveTextContent(myHandler, editText.getText().toString().getBytes());
            }
        }.start();
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
                        FileUtils.get().do_text(ty.mIndex, ty.mPath, App.tempFilePath, 'e');
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
