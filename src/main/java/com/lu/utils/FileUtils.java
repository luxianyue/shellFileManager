package com.lu.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;

import com.lu.App;
import com.lu.model.FileItem;
import com.lu.model.Item;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bulefin on 2017/4/27.
 */

public class FileUtils implements ShellUtil.OnResultListener {

    private static final long KB = 1L << 10;
    private static final long MB = 1L << 20;
    private static final long GB = 1L << 30;
    private static final long TB = 1L << 40;
    private static final long PB = 1L << 50;
    private static final long EB = 1L << 60;

    public static final int FILE_AUDIO = 12;
    public static final int FILE_VIDEO = 13;
    public static final int FILE_IMAGE = 15;
    public static final int FILE_COMPRESS = 31;
    public static final int FILE_GIF = 32;
    public static final int FILE_RAR = 16;
    public static final int FILE_ZIP = 17;
    public static final int FILE_TAR = 18;
    public static final int FILE_JAR = 19;
    public static final int FILE_APK = 20;
    public static final int FILE_TEXT = 21;
    public static final int FILE_SCRIPT = 22;
    public static final int FILE_WORD = 25;
    public static final int FILE_PPT = 26;
    public static final int FILE_XLS = 27;
    public static final int FILE_PDF = 28;
    public static final int FILE_DB = 29;
    public static final int FILE_OTHER = 30;

    private static final int LOAD_FILE_COMPLETE = 100;
    private static final int LOAD_FILE_FAILURE = 101;


    public static final int SORT_BY_FILE_TYPE = 0;
    public static final int SORT_BY_FILE_DATE_ASC = 1;
    public static final int SORT_BY_FILE_DATE_DESC = 2;
    public static final int SORT_BY_FILE_NAME_ASC = 3;
    public static final int SORT_BY_FILE_NAME_DESC = 4;
    public static final int SORT_BY_FILE_SIZE_ASC = 5;
    public static final int SORT_BY_FILE_SIZE_DESC = 6;

    public static int userSortMode;

    private OnLoadFileListener mLoadFileListener;

    private static FileUtils instance;

    private static FileComparator mFileComparator;

    private FileUtils(){
        getShellUtil().setResultListener(this);
        mFileComparator = new FileComparator();
    }

    private static ShellUtil getShellUtil() {
        return ShellUtil.get();
    }
    /*public static FileUtils get() {
        return new FileUtils();
    }*/

    /**
     * 单例模式
     * @return
     */
    public static final FileUtils get() {
        if (instance == null) {
            synchronized (FileUtils.class) {
                if (instance == null) {
                    instance = new FileUtils();
                }
            }
        }
        return instance;
    }

    public void exeCommand(String command) {
        getShellUtil().exeCommand(command);
    }

    /**
     * 列出路径path文件目录里的所有文件
     * @param path
     */
    public void listAllFile(String path) {
        System.out.println("list file--->" + path + "    ===shell-->>" + getShellUtil());
        getShellUtil().exeCommand(App.tools + " -f " + getS(checkString(path)));
        //getShellUtil().exeCommand(App.tools + " -uid");
    }

    public void countDirSize(String path) {
        getShellUtil().exeCommand(App.tools + " -s " + getS(checkString(path)));
    }

    /**
     * 复制文件
     */
    public void copy(String dest, String src) {
        getShellUtil().exeCommand(App.tools + " -cp " + getS(checkString(dest)) + src);
    }

    public void cut(String dest, String src) {
        getShellUtil().exeCommand(App.tools + " -mv " + getS(checkString(dest)) + src);
    }

    public void del(String src) {
        getShellUtil().exeCommand(App.tools + " -del" + src);
    }

    public void chmod(String path, String mode, String fg) {
        //fg only is -n, -d, -r
        getShellUtil().exeCommand(App.tools + " -chm " + fg + " " + mode + path);
    }

    public void do_text(String path, String desPath, char fg) {
        if (fg == 'l') {
            getShellUtil().exeCommand(App.tools + " -ltext " + getS(checkString(path)) + " " + getS(checkString(desPath)));
        }
        if (fg == 'e') {
            getShellUtil().exeCommand(App.tools + " -etext " + getS(checkString(path)) + " " + getS(checkString(desPath)));
        }
    }

    public void mountRW(String dev, String name) {
        getShellUtil().exeCommand(ShellUtil.MOUNT_RW + getS(dev) + " " + getS(name));
    }

    public void mountRO(String dev, String name) {
        getShellUtil().exeCommand(ShellUtil.MOUNT_RO + getS(dev) + " " + getS(name));
    }

    public void createDir(String path) {
        getShellUtil().exeCommand(App.tools + " -nd " + getS(checkString(path)));
    }

    public void createFile(String path) {
        getShellUtil().exeCommand(App.tools + " -nf " + getS(checkString(path)));
    }

    public void rename(String oldN, String newN) {
        getShellUtil().exeCommand(App.tools + " -rn " + getS(checkString(oldN)) + " " + getS(checkString(newN)));
    }

    public static synchronized String getS(String str) {
        return "\"" + str + "\"";
    }

    @Override
    public void onLoadComplete(List<FileItem> list) {
        if (mLoadFileListener != null) {
            sortFileItem(list, userSortMode);
            mLoadFileListener.onLoadComplete(list);
        }
    }

    @Override
    public void onLoadComplete(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onLoadComplete(str);
        }
    }

    @Override
    public void onSizeComplete(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onSizeComplete(str);
        }
    }

    @Override
    public void onRenameComplete(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onRenameComplete(str);
        }
    }

    @Override
    public void onCreateDirComplete(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onCreateDirComplete(str);
        }
    }

    @Override
    public void onCreateFileComplete(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onCreateFileComplete(str);
        }
    }

    @Override
    public void onCpAction(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onCpAction(str);
        }
    }

    @Override
    public void onMvAction(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onMvAction(str);
        }
    }

    @Override
    public void onDelAction(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onDelAction(str);
        }
    }

    @Override
    public void onCHMAction(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onCHMAction(str);
        }
    }

    @Override
    public void onTextAction(String str) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onTextAction(str);
        }
    }

    public void sortFileItem(List<FileItem> list, int which) {
        FileComparator.which = which;
        userSortMode = which;
        if (list != null) {
            Collections.sort(list, mFileComparator);
        }
    }

    @Override
    public void onReqGetRoot(Item item) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onReqGetRoot(item);
        }
    }

    @Override
    public void onError(String msg) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onError(msg);
        }
    }

    public static int[] getFilePermissionNum(String per) {
        int flagSet = 0, owner = 0, user = 0, other = 0;
        int num = 0;
        for (int i = 0; i < per.length(); i++) {
            num = getPerNum(per.charAt(i));
            if (i < 3) {
                if (num != 's' && num != 'S') {
                    owner += num;
                    continue;
                }
                if (num == 's'){
                    flagSet += 4;
                    owner += 1;
                    continue;
                }
                if (num == 'S') {
                    flagSet += 4;
                }
                continue;
            }
            if (i > 2 && i < 6) {
                if (num != 's' && num != 'S') {
                    user += num;
                    continue;
                }
                if (num == 's'){
                    flagSet += 2;
                    user += 1;
                    continue;
                }
                if (num == 'S') {
                    flagSet += 2;
                }
                continue;
            }

            if (num != 't' && num != 'T') {
                other += num;
                continue;
            }
            if (num == 't'){
                flagSet += 1;
                other += 1;
                continue;
            }
            if (num == 'T') {
                flagSet += 1;
            }
        }
        int pers[] = {flagSet, owner, user, other};
        //System.out.println("" + flagSet + owner + user + other);
        return pers;

    }

    public static String getPermissionByMode(String mode) {
        //777
        StringBuffer sb = new StringBuffer();
        sb.append("---------");
        if (mode.length() <= 3) {
            for (int i = 0; i < mode.length(); i++) {
                getCharByNumber(sb, mode.charAt(i), i);
            }
        }
        if (mode.length() == 4) {
            for (int i = 1; i < mode.length(); i++) {
                getCharByNumber(sb, mode.charAt(i), i -1);
            }
            if (mode.charAt(0) != '0') {
                getCharByNumber(sb, mode.charAt(0), -1);
            }
        }
        return sb.toString();
    }

    private static void getCharByNumber(StringBuffer sb, char num, int index) {
        switch (num) {
            case '0':
                if (index != -1) {
                    sb.setCharAt(index * 3, '-');
                    sb.setCharAt(index * 3 + 1, '-');
                    sb.setCharAt(index * 3 + 2, '-');
                }
                break;
            case '1':
                if (index != -1) {
                    sb.setCharAt(index * 3, '-');
                    sb.setCharAt(index * 3 + 1, '-');
                    sb.setCharAt(index * 3 + 2, 'x');
                } else {
                    sb.setCharAt(8, sb.charAt(8) == '-' ? 'T' : 't');
                }
                break;
            case '2':
                if (index != -1) {
                    sb.setCharAt(index * 3, '-');
                    sb.setCharAt(index * 3 + 1, 'w');
                    sb.setCharAt(index * 3 + 2, '-');
                } else {
                    sb.setCharAt(5, sb.charAt(5) == '-' ? 'S' : 's');
                }
                break;
            case '3':
                if (index != -1) {
                    sb.setCharAt(index * 3, '-');
                    sb.setCharAt(index * 3 + 1, 'w');
                    sb.setCharAt(index * 3 + 2, 'x');
                } else {
                    sb.setCharAt(5, sb.charAt(5) == '-' ? 'S' : 's');
                    sb.setCharAt(8, sb.charAt(8) == '-' ? 'T' : 't');
                }
                break;
            case '4':
                if (index != -1) {
                    sb.setCharAt(index * 3, 'r');
                    sb.setCharAt(index * 3 + 1, '-');
                    sb.setCharAt(index * 3 + 2, '-');
                } else {
                    sb.setCharAt(2, sb.charAt(2) == '-' ? 'S' : 's');
                }
                break;
            case '5':
                if (index != -1) {
                    sb.setCharAt(index * 3, 'r');
                    sb.setCharAt(index * 3 + 1, '-');
                    sb.setCharAt(index * 3 + 2, 'x');
                } else {
                    sb.setCharAt(2, sb.charAt(2) == '-' ? 'S' : 's');
                    sb.setCharAt(8, sb.charAt(8) == '-' ? 'T' : 't');
                }
                break;
            case '6':
                if (index != -1) {
                    sb.setCharAt(index * 3, 'r');
                    sb.setCharAt(index * 3 + 1, 'w');
                    sb.setCharAt(index * 3 + 2, '-');
                } else {
                    sb.setCharAt(2, sb.charAt(2) == '-' ? 'S' : 's');
                    sb.setCharAt(5, sb.charAt(5) == '-' ? 'S' : 's');
                }
                break;
            case '7':
                if (index != -1) {
                    sb.setCharAt(index * 3, 'r');
                    sb.setCharAt(index * 3 + 1, 'w');
                    sb.setCharAt(index * 3 + 2, 'x');
                } else {
                    sb.setCharAt(2, sb.charAt(2) == '-' ? 'S' : 's');
                    sb.setCharAt(5, sb.charAt(5) == '-' ? 'S' : 's');
                    sb.setCharAt(8, sb.charAt(8) == '-' ? 'T' : 't');
                }
                break;
        }
    }

    public static String checkString(String name) {
        return name.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int getPerNum(char per) {
        switch (per) {
            case 'r':
                return 4;
            case 'w':
                return 2;
            case 'x':
                return 1;
            case 's':
                return 's';
            case 'S':
                return 'S';
            case 't':
                return 't';
            case 'T':
                return 'T';
            default:
                    return 0;
        }
    }

    public static int getFileType(String name) {
        if (isImageFile(name))    return FILE_IMAGE;
        if (isGifFile(name))      return FILE_GIF;
        if (isAudioFile(name))    return FILE_AUDIO;
        if (isVideoFile(name))    return FILE_VIDEO;
        if (isTextFile(name))     return FILE_TEXT;
        if (isScript(name))       return FILE_SCRIPT;
        if (isApkFile(name))      return FILE_APK;
        if (isCompressFile(name)) return FILE_COMPRESS;
        return FILE_OTHER;
    }

    /**
     * 判断文件是否是图片文件
     * .jpg; .bmp; .eps; .mif; .miff; .png; .tif; .tiff; .svg; .wmf; .jpe; .jpeg; .dib; .ico; .tga; .cut; .pic
     * @param name
     * @return
     */
    public static boolean isImageFile(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".png")
                || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".bmp") || name.endsWith(".svg")) {
            return true;
        }
        return false;
    }

    /**
     * 判断文件是否是gif图片文件
     * .gif
     * @param name
     * @return
     */
    public static boolean isGifFile(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".gif")) {
            return true;
        }
        return false;
    }

    /**
     * 判断文件是否是音频文件
     * .MP3; .AAC; .WAV; .WMA; .CDA; .FLAC; .M4A; .MID; .MKA; .MP2; .MPA; .MPC; .APE; .OFR; .OGG; .RA; .WV; .TTA; .AC3; .DTS
     * M4A,AC3，AMR,APE,FLAC,MID,MP3,OGG,RA,WAV,WMA,ACC,ACC+
     * @param name
     * @return
     */
    public static boolean isAudioFile(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".mp3") || name.endsWith(".aac")
                || name.endsWith(".wav") || name.endsWith(".m4a")
                || name.endsWith(".mid") || name.endsWith(".ape")
                || name.endsWith(".ac3") || name.endsWith(".amr")
                || name.endsWith(".flac") || name.endsWith(".ra")
                || name.endsWith(".wma") || name.endsWith(".ogg")
                || name.endsWith(".midi") || name.endsWith(".acc+")) {
            return true;
        }
        return false;
    }

    /**
     * 判断文件是否是视频文件
     * .AVI; .ASF; .WMV; .AVS; .FLV;.m4v .MKV; .MOV; .3GP; .MP4; .MPG; .MPEG; .DAT; .OGM; .VOB; .RM; .RMVB; .TS; .TP; .IFO; .NSV
     * @param name
     * @return
     */
    public static boolean isVideoFile(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".avi") || name.endsWith(".wmv")
                || name.endsWith(".flv") || name.endsWith(".mkv")
                || name.endsWith(".mov") || name.endsWith(".3pg")
                || name.endsWith(".mp4") || name.endsWith(".mpg")
                || name.endsWith(".mpeg") || name.endsWith(".rm")
                || name.endsWith(".rmvb") || name.endsWith(".asf")
                || name.endsWith(".m4v")) {
            return true;
        }
        return false;
    }

    /**
     * 判断文件是否是压缩文件
     *  .rar*.zip*.cab*.arj*.lzh*.ace*.7-zip*.tar*.gzip*.uue*.bz2*.jar*.iso*.z
     * @param name
     * @return
     */
    public static boolean isCompressFile(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".rar") || name.endsWith(".zip")
                || name.endsWith(".tar") || name.endsWith(".jar")
                || name.endsWith(".gzip") || name.endsWith(".7z")
                || name.endsWith(".tgz") || name.endsWith(".taz")
                || name.endsWith(".z") || name.endsWith(".gz")) {
            return true;
        }
        return false;
    }

    /**
     * 判断文件是否是apk安装包
     * @param name
     * @return
     */
    public static boolean isApkFile(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".apk")) {
            return true;
        }
        return false;
    }

    /**
     * 判断文件是否是文本文件
     *  .txt .sh .log .xml .java .c .cpp .mk .ini .inf .h
     * @param name
     * @return
     */
    public static boolean isTextFile(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".txt")
                || name.endsWith(".conf") || name.endsWith(".inf")
                || name.endsWith(".log") || name.endsWith(".xml")
                || name.endsWith(".java") || name.endsWith(".c")
                || name.endsWith(".cpp") || name.endsWith(".mk")
                || name.endsWith(".h") || name.endsWith(".ini")) {
            return true;
        }
        return false;
    }

    public static boolean isScript(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".sh") || name.endsWith(".rc")) {
            return true;
        }
        return false;
    }

    /**
     * 对字节数进行相应的格式化，比如1024个字节=1k
     * @param byteNumber
     * @return
     */
    public static String getFormatByte(long byteNumber) {
        String formatByte = "0字节";
        if (byteNumber < KB) {
            formatByte = byteNumber + "字节";
        } else if (byteNumber < MB && byteNumber >= KB) {
            formatByte = toSaveTwoDot(byteNumber * 1d / KB, 2) + "K";
        } else if (byteNumber < GB && byteNumber >= MB) {
            formatByte = toSaveTwoDot(byteNumber * 1d / MB, 2) + "M";
        } else if (byteNumber < TB && byteNumber >= GB) {
            formatByte = toSaveTwoDot(byteNumber * 1d / GB, 2) + "G";
        } else if (byteNumber < PB && byteNumber >= TB) {
            formatByte = toSaveTwoDot(byteNumber * 1d / TB, 2) + "T";
        } else if (byteNumber < EB && byteNumber >= PB) {
            formatByte = toSaveTwoDot(byteNumber * 1d / PB, 2) + "P";
        } else if (byteNumber >= EB) {
            formatByte = toSaveTwoDot(byteNumber * 1d / EB, 2) + "E";
        }

        return formatByte;
    }

    /**
     * 对浮点数保留digit位小数点，直接截取，不进行四舍五入 对于整数则不进行处理，保留原数
     *
     * @param number
     *            输入的浮点数
     * @param digit
     *            要保留的小数点位数
     * @return
     */
    private static String toSaveTwoDot(double number, int digit) {
        String str = number + "";
        if (str.substring(str.indexOf(".") + 1).length() < digit) {
            if (Integer.parseInt(str.substring(str.indexOf(".") + 1)) < 1) {
                str = str.substring(0, str.indexOf("."));
            }
        } else {
            str = str.substring(0, str.indexOf(".") + (digit + 1));
        }

        return str;
    }

    public static int getDotForSpill(float number, int digit) {
        String str = number + "";
        if (str.substring(str.indexOf(".") + 1).length() < digit) {
            if (Integer.parseInt(str.substring(str.indexOf(".") + 1)) < 1) {
                str = str.substring(0, str.indexOf("."));
            }
        } else {
            str = str.substring(0, str.indexOf(".") + (digit + 1));
        }

        return Integer.parseInt(str);
    }

    static class FileComparator implements Comparator<FileItem> {
        public static int which = SORT_BY_FILE_TYPE;

        @Override
        public int compare(FileItem first, FileItem second) {
            if (first.isUpper) {
                return -1;
            }
            if (second.isUpper) {
                return 1;
            }
            switch (which) {
                case SORT_BY_FILE_TYPE:
                    //类型
                    if (first.isFolder() && second.isFolder()) {
                        return first.getName().toLowerCase().compareTo(second.getName().toLowerCase());
                    }
                    if (first.isFolder() && !second.isFolder()) {
                        return -1;
                    }
                    if (!first.isFolder() && second.isFolder()) {
                        return 1;
                    }
                    int index = first.getName().lastIndexOf(".");
                    int index2 = second.getName().lastIndexOf(".");
                    if (index != -1 && index2 == -1) {
                        return 1;
                    }
                    if (index == -1 && index2 != -1) {
                        return -1;
                    }

                    return first.getName().substring(index + 1).toLowerCase().compareTo(second.getName().substring(index2 + 1).toLowerCase());
                case SORT_BY_FILE_DATE_ASC:
                    //日期 升序
                    if (first.lastModified() < second.lastModified()) {
                        return -1;
                    }
                    if (first.lastModified() > second.lastModified()) {
                        return 1;
                    }
                    return 0;
                case SORT_BY_FILE_DATE_DESC:
                    //日期降序
                    if (first.lastModified() < second.lastModified()) {
                        return 1;
                    }
                    if (first.lastModified() > second.lastModified()) {
                        return -1;
                    }
                    return 0;
                case SORT_BY_FILE_NAME_ASC:
                    //名称 升序
                    return first.getName().toLowerCase().compareTo(second.getName().toLowerCase());
                case SORT_BY_FILE_NAME_DESC:
                    //名称降序
                    return second.getName().toLowerCase().compareTo(first.getName().toLowerCase());
                case SORT_BY_FILE_SIZE_ASC:
                    //大小 升序
                    if (first.size() < second.size()) {
                        return -1;
                    }
                    if (first.size() > second.size()) {
                        return 1;
                    }
                    return 0;
                case SORT_BY_FILE_SIZE_DESC:
                    //大小降序
                    if (first.size() < second.size()) {
                        return 1;
                    }
                    if (first.size() > second.size()) {
                        return -1;
                    }
                    return 0;
                default:
                    return 0;
            }
        }
    }

    public static boolean isStop;
    public static void lookTextContent(Handler handler, String path) {
        isStop = false;
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = new FileInputStream(new File(path));
            bis = new BufferedInputStream(is);
            int len = 0;
            byte buf[] = new byte[4 * 1024];
            while ((len = bis.read(buf)) != -1 && !isStop) {
                if (handler != null) {
                    handler.obtainMessage(1, new String(buf, 0, len)).sendToTarget();
                    Thread.sleep(100);
                }
            }
            handler.sendEmptyMessage(-1);
            isStop = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            bis = null;
            is = null;
        }
    }

    public static void saveTextContent(Handler handler, byte[] content) {
        OutputStream os = null;
        BufferedOutputStream bos = null;
        try {
            os = new FileOutputStream(new File(App.tempFilePath));
            bos = new BufferedOutputStream(os);
            bos.write(content, 0, content.length);
            handler.sendEmptyMessage(2);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            bos = null;
            os = null;
        }
    }

    /**
     * 通过反射调用获取内置存储和外置sd卡根路径(通用)
     * @return
     */
    public static List<String> getStoragePath(Context context) {
        StorageManager mStorageManager = (StorageManager) context.getApplicationContext().getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        List<String> pathList = new ArrayList<>();
        pathList.add(0, null);
        pathList.add(1, null);
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            String phonePath = Environment.getExternalStorageDirectory().toString();

            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                if (!new File(path).exists()) {
                    continue;
                }
                if (phonePath.equals(path)) {
                    pathList.set(0, path);
                    continue;
                }
                int index = path.lastIndexOf("/");
                if (path.substring(index + 1).toLowerCase().contains("sdcard")) {
                    if (pathList.get(1) == null) {
                        pathList.set(1, path);
                        continue;
                    }
                }
                pathList.add(path);
                //boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pathList;
    }

    /**
     * 加载文件时状态的接口
     */
    public interface OnLoadFileListener {
        void onLoadComplete(List<FileItem> items);
        void onLoadComplete(String str);
        void onSizeComplete(String str);
        void onRenameComplete(String str);
        void onCreateDirComplete(String str);
        void onCreateFileComplete(String str);
        void onCpAction(String str);
        void onMvAction(String str);
        void onDelAction(String str);
        void onCHMAction(String str);
        void onTextAction(String str);
        void onReqGetRoot(Item item);
        void onError(String msg);
    }

    public void setOnLoadFileListener(OnLoadFileListener loadFileListener) {
        this.mLoadFileListener = loadFileListener;
    }
}
