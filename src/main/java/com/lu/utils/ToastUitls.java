package com.lu.utils;

import android.view.Gravity;
import android.widget.Toast;

import com.lu.App;

/**
 * Created by bulefin on 2017/11/15.
 */

public class ToastUitls {
    public static void showLMsg(String msg) {
        Toast.makeText(App.context(), msg, Toast.LENGTH_LONG).show();
    }

    public static void showSMsg(String msg) {
        Toast.makeText(App.context(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLMsgAtCenter(String msg) {
        Toast toast = Toast.makeText(App.context(), msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showSMsgAtCenter(String msg) {
        Toast toast = Toast.makeText(App.context(), msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
