package com.lu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.lu.App;
import com.lu.filemanager2.R;

/**
 * Created by bulefin on 2017/10/31.
 */

public class SharePreferenceUtils {
    private static SharedPreferences mSp;
    public static void init(Context context) {
        if (mSp == null) {
            mSp = context.getApplicationContext().getSharedPreferences("config", Context.MODE_PRIVATE);
        }
    }

    public static void saveFragmentCount(int count) {
        mSp.edit().putInt("ft_count", count).commit();
    }

    public static int getFragmentCount() {
        return mSp.getInt("ft_count", 1);
    }

    public static int getVisibleIndex() {
        return mSp.getInt("visible_index", 0);
    }

    public static String getCurrentPath(int index) {
        return mSp.getString("tab_s_" + index, "/");
    }

    public static String getInitDir(int index) {
        return mSp.getString("tab_initd_" + index, null);
    }

    public static String getCurrentPathName(int index) {
        String path = getCurrentPath(index);
        String name[] = path.split("/");
        if (name.length <= 0) {
            return App.context().getString(R.string.root_dir);
        }
        if (Environment.getExternalStorageDirectory().getAbsolutePath().equals(path)) {
            return App.context().getString(R.string.phone_storage);
        }
        return name[name.length - 1];
    }

    public static void clearFragmentState(int index) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.remove("tab_" + index);
        editor.remove("tab_initd_" + index);
        editor.remove("tab_s_" + index);
        editor.remove("tab_b_" + index);
        editor.remove("fp_" + index);
        editor.remove("tp_" + index);
        //editor.remove("visible_index");
        editor.commit();
    }

    public static void saveFragmentState(int index, String initDir, String currentPath, boolean isShowToUser) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putInt("tab_" + index, index);
        editor.putString("tab_s_" + index, currentPath);
        editor.putBoolean("tab_b_" + index, isShowToUser);
        if (initDir != null) {
            editor.putString("tab_initd_" + index, initDir);
        }
        if (isShowToUser) {
            editor.putInt("visible_index", index);
        }
        editor.commit();
    }

    public static void saveFragmentVisibleIndex(int index) {
        mSp.edit().putInt("visible_index", index).commit();
    }

    public static String[] getSavedFragmentState(int index) {
        int ind = mSp.getInt("tab_" + index, -1);
        String path = getCurrentPath(index);
        String initDir = getInitDir(index);
        boolean b = mSp.getBoolean("tab_b_" + index, false);
        String str[] = {ind + "", path, initDir, Boolean.toString(b)};
        return str;
    }

    public static void saveListViewFpAndTp(int index, int firstPosition, int top) {
        mSp.edit().putInt("fp_" + index, firstPosition).commit();
        mSp.edit().putInt("tp_" + index, top).commit();
    }

    public static int getListViewFirstPos(int index) {
        return mSp.getInt("fp_" + index, 0);
    }

    public static int getListViewTop(int index) {
        return mSp.getInt("tp_" + index, 0);
    }

    public static void saveFileSort(int which) {
        mSp.edit().putInt("sort", which).commit();
    }

    public static int getFileSortMode() {
        return mSp.getInt("sort", 0);
    }

    public static int getFileSortButtonId() {
        switch (mSp.getInt("sort", 0)){
            case 0:
                return R.id.sort_type;
            case 1:
                return R.id.sort_date_asc;
            case 2:
                return R.id.sort_date_desc;
            case 3:
                return R.id.sort_name_asc;
            case 4:
                return R.id.sort_name_desc;
            case 5:
                return R.id.sort_size_asc;
            case 6:
                return R.id.sort_size_desc;
        }
        return R.id.sort_type;
    }
}
