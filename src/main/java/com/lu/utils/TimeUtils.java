package com.lu.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bulefin on 2017/11/1.
 */

public class TimeUtils {
    private static SimpleDateFormat mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static Date mDate = new Date();

    public static String getFormatDateTime(long time) {
        mDate.setTime(time);
        return mSdf.format(mDate);
    }
}
