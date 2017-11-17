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
    public static Object[] isOnlyReadFileSys(String absolutePath) {
        Object obj[] = {true, "rootfs", "/"};
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
                            obj[1] = str.substring(0, index).trim();
                            obj[2] = fname;
                            //System.out.println(str.substring(0, index).trim());
                            if ("rw".equals(row)) {
                                obj[0] = false;
                                return obj;
                            } else {
                                obj[0] = true;
                                return obj;
                            }
                        }
                    }
                }
            }
            obj[0] = false;
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
        return obj;
    }

    public static String getFs(String path) {
        String str[] = path.split("/");
        if (str.length > 2) {
            return "/" + str[1];
        }
        return "/";
    }
}
