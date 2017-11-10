package com.lu.filemanager2;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.lu.activity.BasedActivity;

/**
 * Created by bulefin on 2017/10/31.
 */

public class MainActivityTest extends BasedActivity implements View.OnClickListener {

    static {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //System.loadLibrary("test-jni");
        //test(10010);
    }

    public static void callJava(){
        Log.e("callJava", "this is a javaMethod");
    }
    public native void test(int uid);

    public native void setUid(int uid);

    @Override
    public void onClick(View v) {
        setUid(111);
    }
}
