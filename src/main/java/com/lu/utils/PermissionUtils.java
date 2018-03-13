package com.lu.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bulefin on 2017/11/14.
 */

public class PermissionUtils {

    public static int index;
    private static String sysName;
    public static String arrays[] = {"", "", "", null};

    public static void setCheckPath(String path) {
        PermissionUtils.sysName = getFs(path);
    }

    public static boolean parseSys(String str) {
        String strs[] = str.split("[ ]+");
        //System.out.println("sysName:" + sysName + " (array:" + strs[1]);
        if ("/".equals(strs[1])) {
            arrays[0] = strs[3].substring(0,2);
            arrays[1] = strs[0];
            arrays[2] = strs[1];
            arrays[3] = strs[2];
            if ("rw".equals(arrays[0])) {
                arrays[0] = "false";
            }
            if ("ro".equals(arrays[0])) {
                arrays[0] = "true";
            }
        }
        if ("/".equals(sysName)) {
            if (strs[1].equals(sysName)) {
                arrays[0] = strs[3].substring(0,2);
                arrays[1] = strs[0];
                arrays[2] = strs[1];
                arrays[3] = strs[2];
                if ("rw".equals(arrays[0])) {
                    arrays[0] = "false";
                }
                if ("ro".equals(arrays[0])) {
                    arrays[0] = "true";
                }
                return true;
            }
        } else {
            if (sysName.contains(strs[1]) && sysName.startsWith(strs[1]) && sysName.charAt(strs[1].length()) == '/') {
                arrays[0] = strs[3].substring(0,2);
                arrays[1] = strs[0];
                arrays[2] = strs[1];
                arrays[3] = strs[2];
                if ("rw".equals(arrays[0])) {
                    arrays[0] = "false";
                }
                if ("ro".equals(arrays[0])) {
                    arrays[0] = "true";
                }
                return true;
            }
        }
        if ("mount end".equals(str)) {
            return true;
        }
        return false;
    }

    public static String[] isOnlyReadFileSys2(String absolutePath) {
        String strs[] = {"true", "rootfs", "/", null};
        String path = "/proc/mounts";
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            is = new FileInputStream(path);
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String str = null;
            String array[] = null;
            while ((str = br.readLine()) != null) {
                array = str.split("[ ]+");
                if (array[1].equals(getFs(absolutePath))) {
                    strs[0] = array[3].substring(0,2);
                    strs[1] = array[0];
                    strs[2] = array[1];
                    strs[3] = array[2];
                    if ("rw".equals(strs[0])) {
                        strs[0] = "false";
                    }
                    if ("ro".equals(strs[0])) {
                        strs[0] = "true";
                    }
                    return strs;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                isr.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return strs;
    }

    public static String[] isOnlyReadFileSys(String absolutePath) {
        String strs[] = {"true", "rootfs", "/", null};
        String path = "/proc/mounts";
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            is = new FileInputStream(path);
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String str = null;
            Pattern patternName = Pattern.compile("\\s/\\S*\\s");
            Pattern patternRW = Pattern.compile("\\sr[ow],");
            while ((str = br.readLine()) != null) {
                Matcher matcher = patternName.matcher(str);
                if (matcher.find()) {
                    String name = matcher.group().trim();
                    int index = matcher.start();
                    //System.out.println(getFs(absolutePath) + "=============>" + name);
                    String fname = getFs(absolutePath);
                    if (name.startsWith(fname)) {
                        //System.out.println("=============>>");
                        matcher = patternRW.matcher(str);
                        if (matcher.find()) {
                            String row = matcher.group().trim().substring(0, 2);
                            strs[1] = str.substring(0, index).trim();
                            strs[2] = fname;
                            //System.out.println(str.substring(0, index).trim());
                            if ("rw".equals(row)) {
                                strs[0] = "false";
                                return strs;
                            } else {
                                strs[0] = "true";
                                return strs;
                            }
                        }
                    }
                }
            }
            strs[0] = "false";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                isr.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return strs;
    }

    public static String getFs(String path) {
        // /system/aa.txt
        String str[] = path.split("/");
        if (str.length < 3) {
            return "/";
        }
        return path;
    }
}
