package com.lu.utils;

import android.os.Handler;
import android.os.Message;

import com.lu.filemanager2.R;
import com.lu.model.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bulefin on 2017/4/27.
 */

public class FileUtil2 {

    private static final long KB = 1l << 10;
    private static final long MB = 1l << 20;
    private static final long GB = 1l << 30;
    private static final long TB = 1l << 40;
    private static final long PB = 1l << 50;
    private static final long EB = 1l << 60;

    private static final int LOAD_FILE_COMPLETE = 100;
    private static final int LOAD_FILE_FAILURE = 101;

    private OnLoadFileListener mLoadFileListener;

    private List<FileItem> mItemList;

    private FileUtil2(){
        this.mItemList = new ArrayList<>();
    }

    public static FileUtil2 getInstance() {
        return new FileUtil2();
    }

    /**
     * 列出路径path文件目录里的所有文件
     * @param path
     */
    public void listAllFile(String path) {
        new LoadFileThread(path).start();
    }

    /**
     * 列出路径path文件目录里的所有文件
     * @param path
     */
    private void listFile(String path) {
        File filePath = new File(path);
        if (!filePath.exists() || !filePath.canRead()) {
            mHandler.sendEmptyMessage(LOAD_FILE_FAILURE);
            return;
        }

        mItemList.clear();
        FileItem item = null;
        for(File file : filePath.listFiles()) {
            item = new FileItem();
            //item.setName(file.getName());
            //item.setFolder(file.isDirectory());
            //item.setTime(file.lastModified()+"");
            //item.setAbsolutePath(file.getAbsolutePath());
            if (file.isDirectory()) {
                //item.setIcon(R.drawable.folder_blue);
                //item.setSize(file.canRead() ? file.list().length + "项" : "0项");
            } else {
                if (isImage(file.getName())) {
                    if (!file.getName().toLowerCase().endsWith(".gif")) {
                        //item.setType(FileUtil.FILE_IMAGE);
                    } else {
                        //item.setType(FileUtil.FILE_GIF);
                    }
                }

                if (isAudio(file.getName())) {
                    //item.setType(FileUtil.FILE_AUDIO);
                }

                if (isVideo(file.getName())) {
                    //item.setType(FileUtil.FILE_VIDEO);
                }

                if (isCompress(file.getName())) {
                    //item.setType(FileUtil.FILE_COMPRESS);
                }

                if (isText(file.getName())) {
                    //item.setType(FileUtil.FILE_TEXT);
                }

                if (file.getName().toLowerCase().endsWith(".apk")) {
                    //item.setType(FileUtil.FILE_APK);
                }
                //item.setIcon(R.drawable.unknown);
                //item.setSize(getFormatByte(file.length()));
            }
            mItemList.add(item);
        }
        mHandler.sendEmptyMessage(LOAD_FILE_COMPLETE);
    }

    /**
     * handler,主要用于线程间通信，子线程与UI线程的通信
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_FILE_COMPLETE:
                    if (mLoadFileListener != null) {
                        mLoadFileListener.onComplete(mItemList);
                    }
                    break;
                case LOAD_FILE_FAILURE:
                    mLoadFileListener.onComplete(null);
                    break;
            }
        }
    };

    /**
     * 列出文件的线程类
     */
    class LoadFileThread extends Thread {
        private String path;
        LoadFileThread (String path) {
            this.path = path;
        }
        @Override
        public void run() {
            listFile(path);
        }
    }

    /**
     * 判断文件是否是图片文件
     * .jpg; .bmp; .eps; .gif; .mif; .miff; .png; .tif; .tiff; .svg; .wmf; .jpe; .jpeg; .dib; .ico; .tga; .cut; .pic
     * @param name
     * @return
     */
    private boolean isImage(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".png")
                || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".bmp") || name.endsWith(".gif")) {
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
    private boolean isAudio(String name) {
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
     * .AVI; .ASF; .WMV; .AVS; .FLV; .MKV; .MOV; .3GP; .MP4; .MPG; .MPEG; .DAT; .OGM; .VOB; .RM; .RMVB; .TS; .TP; .IFO; .NSV
     * @param name
     * @return
     */
    private boolean isVideo(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".avi") || name.endsWith(".wmv")
                || name.endsWith(".flv") || name.endsWith(".mkv")
                || name.endsWith(".mov") || name.endsWith(".3pg")
                || name.endsWith(".mp4") || name.endsWith(".mpg")
                || name.endsWith(".mpeg") || name.endsWith(".rm")
                || name.endsWith(".rmvb") || name.endsWith(".asf")) {
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
    private boolean isCompress(String name) {
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
     * 判断文件是否是文本文件
     *  .txt .sh .log .xml .java .c .cpp .mk .ini .inf .h
     * @param name
     * @return
     */
    private boolean isText(String name) {
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
        } else if (byteNumber == EB) {
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

    /**
     * 加载文件时状态的接口
     */
    public interface OnLoadFileListener {
        void onComplete(List<FileItem> items);
    }

    public void setOnLoadFileListener(OnLoadFileListener loadFileListener) {
        this.mLoadFileListener = loadFileListener;
    }
}
