package com.lu.utils;

import android.content.Context;
import android.content.SharedPreferences;

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
