package com.lu.filemanager2;

import android.os.Bundle;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.lu.utils.FileUtil;
import com.lu.utils.ShellUtil;

/**
 * Created by bulefin on 2017/9/8.
 */

public class TestActivity extends AppCompatActivity implements View.OnClickListener {

    EditText et,et2;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main666);
        et = (EditText) findViewById(R.id.et);
        et2 = (EditText) findViewById(R.id.et2);
        findViewById(R.id.bt).setOnClickListener(this);
        findViewById(R.id.bt2).setOnClickListener(this);//12397 12415
        System.out.println(Process.myPid());
        FileUtil.getInstance().getmShellUtil().exeCommand("echo $$\n");
    }

    @Override
    public void onClick(View v) {
        ShellUtil shellUtil = FileUtil.getInstance().getmShellUtil();
        if (v.getId() == R.id.bt) {
            shellUtil.exeCommand(et.getText().toString());
        } else {
            shellUtil.exeCommand(et2.getText().toString());
        }
    }
}
