package com.lu.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by bulefin on 2017/12/12.
 */

public class BuildUtils {
    private static final String systemPropPath = "/system/build.prop";
    private static Properties mPP = new Properties();

    static {
        try {
            mPP.load(new FileInputStream(systemPropPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static final String CPU_ABI = getString("ro.product.cpu.abi");

    public static final String CPU_ABI2 = getString("ro.product.cpu.abi2");

    public static final String[] SUPPORTED_ABIS = getStringArray("ro.product.cpu.abilist", ",");

    public static final String[] SUPPORTED_32_BIT_ABIS = getStringArray("ro.product.cpu.abilist32", ",");

    public static final String[] SUPPORTED_64_BIT_ABIS = getStringArray("ro.product.cpu.abilist64", ",");

    private static String getString(String property) {
        return mPP.getProperty(property);
    }

    private static String[] getStringArray(String property, String split) {
        return mPP.getProperty(property) != null ? mPP.getProperty(property).split(split) : null;
    }

}
