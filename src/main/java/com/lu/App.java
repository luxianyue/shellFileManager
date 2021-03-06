package com.lu;

import android.app.Activity;
import android.content.Context;

import com.lu.utils.BuildUtils;
import com.lu.utils.SharePreferenceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bulefin on 2017/8/29.
 */

public class App extends android.app.Application {

    protected static List<Activity> mActivities;
    public static String exePath;
    public static String tools;
    public static String tempFilePath;

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mActivities = new ArrayList<>();
        initTempFile();
        exePath = getFilesDir().getParentFile().getAbsolutePath();
        SharePreferenceUtils.init(this);
    }

    public static Context context() {
        return mContext;
    }

    public static void initTempFile() {
        tempFilePath = mContext.getFilesDir().getAbsolutePath() + "/temp";
        File temp = new File(tempFilePath);
        if (!temp.exists()) {
            try {
                if (temp.createNewFile()) {
                    temp.setReadable(true, false);
                    temp.setWritable(true, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            temp.setReadable(true, false);
            temp.setWritable(true, false);
        }

    }

    public static boolean initTools() {
        InputStream fis = null;
        OutputStream out = null;
        try {
            /*File file = new File(exePath + "/error");
            if (!file.isFile()) {
                file.createNewFile();
            }*/
            if (new File(exePath + "/tools").exists()) {
                tools = exePath + "/tools";
                return true;
            }


            fis = mContext.getAssets().open("libs/" + BuildUtils.CPU_ABI + "/tools");
            out = new FileOutputStream(exePath + "/tools");
            int len = 0;
            byte buf[] = new byte[1024];
            while ((len = fis.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            File exe = new File(exePath + "/tools");
            if (exe.exists()) {
                exe.setReadable(true, false);
                exe.setWritable(true, false);
                exe.setExecutable(true, false);
                tools = exe.getAbsolutePath();
                return true;
            }
            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                out = null;
                fis = null;
            }
        }
        return false;
    }

    public static List<Activity> getActivities() {
        return mActivities;
    }

    public static void finishAllActivity() {
       /* for (Activity activity : mActivities) {
            mActivities.remove(activity);
            activity.finish();
        }
        if (mActivities.size() > 0) {
            mActivities.clear();
        }*/
    }
}
