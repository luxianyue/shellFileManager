package com.lu.utils;

import com.lu.App;
import com.lu.model.FileItem;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bulefin on 2017/4/27.
 */

public class FileUtil implements ShellUtil.OnResultListener {

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

    private static ShellUtil mShellUtil;

    private static FileUtil instance;

    private static FileComparator mFileComparator;

    private FileUtil (){
        mShellUtil = ShellUtil.getInstance();
        mShellUtil.setResultListener(this);
        mFileComparator = new FileComparator();
    }

    public ShellUtil getmShellUtil() {
        return mShellUtil;
    }
    /*public static FileUtil getInstance() {
        return new FileUtil();
    }*/

    /**
     * 单例模式
     * @return
     */
    public static final FileUtil getInstance() {
        if (instance == null) {
            synchronized (FileUtil.class) {
                if (instance == null) {
                    instance = new FileUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 列出路径path文件目录里的所有文件
     * @param path
     */
    public void listAllFile(String path) {
        mShellUtil.exeCommand(App.myls + " -f \"" + path + "\"");
        //mShellUtil.exeCommand("myls " + path);
    }

    /**
     * 复制文件
     */
    public void copy(String src, String dest) {
        mShellUtil.exeCommand("cp -r \"" + src + "\" \"" + dest + "\"");
    }

    public void cut(String src, String dest) {
        mShellUtil.exeCommand("mv \"" + src + "\" \"" + dest + "\"");
    }

    public void del(String src) {
        mShellUtil.exeCommand("rm -rf \"" + src + "\"");
    }

    @Override
    public void onLoadComplet(List<FileItem> list) {
        if (mLoadFileListener != null) {
            sortFileItem(list, userSortMode);
            mLoadFileListener.onLoadComplete(list);
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
    public void onError(String msg) {
        if (mLoadFileListener != null) {
            mLoadFileListener.onError(msg);
        }
    }

    public static int getFileType(String name) {
        if (isImageFile(name))    return FILE_IMAGE;
        if (isGifFile(name))      return FILE_GIF;
        if (isAudioFile(name))    return FILE_AUDIO;
        if (isVideoFile(name))    return FILE_VIDEO;
        if (isTextFile(name))     return FILE_TEXT;
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
        if (name.endsWith(".txt") || name.endsWith(".sh")
                || name.endsWith(".log") || name.endsWith(".xml")
                || name.endsWith(".java") || name.endsWith(".c")
                || name.endsWith(".cpp") || name.endsWith(".mk")
                || name.endsWith(".h") || name.endsWith(".ini")) {
            return true;
        }
        return false;
    }

    public static String getFileCountOrSize(boolean isFolder, long size, long count) {
        if (isFolder) {
            return count + "项";
        } else {
            return getFormatByte(size);
        }
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

    static class FileComparator implements Comparator<FileItem> {
        public static int which = SORT_BY_FILE_TYPE;

        @Override
        public int compare(FileItem first, FileItem second) {
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

    /**
     * 加载文件时状态的接口
     */
    public interface OnLoadFileListener {
        void onLoadComplete(List<FileItem> items);
        void onError(String msg);
    }

    public void setOnLoadFileListener(OnLoadFileListener loadFileListener) {
        this.mLoadFileListener = loadFileListener;
    }
}
