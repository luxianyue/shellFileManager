package com.lu.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.os.Message;

import com.lu.filemanager2.R;
import com.lu.model.FileItem;

public class ShellUtil {

    private static final String
            LS_FILE_ALL = "ls -al",
            LS_FILE_EXCEPT_HIDE = "ls -l",
            LS_FILE_ALL_FOR_LINK = "ls -aF",
            LS_FILE_ALL_FOR_LINK_EXCEPT_HIDE = "ls -F",
            CD = "cd",
            SU = "su",
            PWD = "pwd",
            SHELL = "sh";

    public static final int
            FLAG_LS_FILE_ALL = 1,
            FLAG_LS_FILE_EXCEPT_HIDE = 2;


    private static volatile ShellUtil instance;

    private Process mProcess;

    private InputStreamReader mReader, mErrorReader;
    private BufferedReader mBr, mErrorBr;
    private InputStream mInStream, mErrorStream;
    private OutputStream mOutStream;

    private MyHandler mHandler;

    private OnResultListener resultListener;

    private String currentCommand;
    private String permissionCommand;

    private boolean isGetRoot = true;


    //时间
    static Pattern patternTime = Pattern.compile("\\s\\d{2}:\\d{2}\\s");
    //年月日
    static Pattern patternYear = Pattern.compile("\\s\\d{4}-\\d{2}-\\d{2}\\s");
    //大小
    static Pattern patternSize = Pattern.compile("\\s[1-9]\\d*\\s|0\\s$");
    //链接
    static Pattern patternLink = Pattern.compile("\\s->\\s");

    Matcher matcher;

    /**
     * 缓冲字节数组的大小  2*1024
     */
    private static final int BUF_SIZE = 1 << 10;

    /**
     * 用来接收命令执行后的(输出)结果
     */
    private String result;

    private List<FileItem> fileList;

    private Queue<String> commandQueue;


    private ShellUtil() {
        mHandler = new MyHandler(this);
        commandQueue = new LinkedList<>();
        //把正常的结果输出流和错误流合并在同一个流里面
        //processBuilder.redirectErrorStream(true);
        try {
            //申请获取终端
            mProcess = Runtime.getRuntime().exec(SHELL);
            mInStream = mProcess.getInputStream();
            mErrorStream = mProcess.getErrorStream();
            mOutStream = mProcess.getOutputStream();
            //开启线程用来处理命令执行后的相关信息，正常结果和错误结果
            new Thread(new ResultThread(this, 1)).start();
            new Thread(new ResultThread(this, 2)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 单例模式
     * @return
     */
    public static final ShellUtil getInstance() {
		if (instance == null) {
			synchronized (ShellUtil.class) {
				if (instance == null) {
					instance = new ShellUtil();
				}
			}
		}
		return instance;
	}

    /**
     * 多例
     *
     * @return
     */
    /*public static final ShellUtil getInstance() {
        return new ShellUtil();
    }*/

    /**
     * 执行用户输入的命令
     *
     * @param command
     */
    public synchronized void exeCommand(String command) {
        try {
            if (mOutStream != null) {
                currentCommand = command;
                mOutStream.write((command + "\n").getBytes());
                if (currentCommand.startsWith("cd ") || currentCommand.equals(SU)){
                    mOutStream.write(("echo $?\n").getBytes());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 读取输入流的结果
     *
     */
    private void readStream() {
        try {
            String content = "";
            if (mReader == null) {
                mReader = new InputStreamReader(mInStream);
            }
            if (mBr == null) {
                mBr = new BufferedReader(mReader);
            }

            List<FileItem> itemList = null;
            List<FileItem> linkFiles = null;
            FileItem item;
            boolean isHasLink = false;
            boolean isNull = true;
            String currentPath = null;
            while ((content = mBr.readLine()) != null) {
                //System.out.println("command-->:" + currentCommand + " --conten--->:" +content);
                if (currentCommand.equals(LS_FILE_EXCEPT_HIDE)){
                    char type = content.charAt(0);
                    if (isNull) {
                        itemList = new ArrayList<>();
                        isNull = false;
                    }
                    item = new FileItem();

                    matcher = patternTime.matcher(content);
                    if (matcher.find()){
                        item.setTime(matcher.group().trim());
                        item.setName(content.substring(matcher.end()).trim());
                    }

                    matcher = patternYear.matcher(content);
                    if (matcher.find()) {
                        item.setDate(matcher.group().trim());
                    }

                    // ("d":目录，"-":文件;"c":字符型设备;"b":块设备; "l" 链接类型)
                    if (type == 'd'){
                        //文件夹
                        item.setFolder(true);
                        item.setIcon(R.drawable.folder_blue);
                    } else if (type == '-'){
                        //文件
                        item.setIcon(R.drawable.unknown);
                        matcher = patternSize.matcher(content);
                        if (matcher.find()) {
                            item.setSize(FileUtil.getFormatByte(Long.parseLong(matcher.group().trim())));
                        }
                        if (FileUtil.isImageFile(item.getName())) {
                            item.setType(FileUtil.FILE_IMAGE);
                        } else if (FileUtil.isGifFile(item.getName())) {
                            item.setType(FileUtil.FILE_GIF);
                        } else if (FileUtil.isAudioFile(item.getName())) {
                            item.setType(FileUtil.FILE_AUDIO);
                        } else if (FileUtil.isVideoFile(item.getName())) {
                            item.setType(FileUtil.FILE_VIDEO);
                        } else if (FileUtil.isTextFile(item.getName())) {
                            item.setType(FileUtil.FILE_TEXT);
                        } else if (FileUtil.isApkFile(item.getName())) {
                            item.setType(FileUtil.FILE_APK);
                        } else if (FileUtil.isCompressFile(item.getName())) {
                            item.setType(FileUtil.FILE_COMPRESS);
                        }
                    } else if (type == 'l') {
                        //链接文件
                        isHasLink = true;
                        item.setLink(true);
                        item.setIcon(R.drawable.unknown);
                        matcher = patternLink.matcher(item.getName());
                        if (matcher.find()) {
                            //   sdcard -> /mnt/sdcard
                            item.setLinkTo(item.getName().substring(matcher.end()));
                            item.setName(item.getName().substring(0, matcher.start()));
                        }
                        if (linkFiles == null) {
                            linkFiles = new ArrayList<>();
                        }
                        linkFiles.add(item);
                    } else if (type == 'c') {
                        item.setIcon(R.drawable.unknown);
                    } else if (type == 'b') {
                        item.setIcon(R.drawable.unknown);
                    } else {
                        item.setIcon(R.drawable.unknown);
                    }

                    if ("/".equals(currentPath)) {
                        item.setPath(currentPath + item.getName());
                    } else {
                        item.setPath(currentPath + "/" + item.getName());
                    }

                    itemList.add(item);
                } else if (currentCommand.equals(LS_FILE_ALL_FOR_LINK_EXCEPT_HIDE)) {
                    if (content.substring(0, 2).equals("ld")) {
                        for (FileItem fitem : linkFiles) {
                            if (fitem.getName().equals(content.substring(3))) {
                                fitem.setLinkPath(true);
                                fitem.setIcon(R.drawable.folder_blue);
                                break;
                            }
                        }
                    }

                }

                if (!mBr.ready()) {
                    if (currentCommand.startsWith("cd ")){
                        if (content.charAt(0) == '0') {
                            exeCommand(PWD);
                        }
                    } else if (currentCommand.equals(PWD)) {
                        currentPath = content;
                        exeCommand(LS_FILE_EXCEPT_HIDE);
                    } else if(currentCommand.equals(LS_FILE_EXCEPT_HIDE)){
                        if (isHasLink) {
                            isHasLink = false;
                            exeCommand(LS_FILE_ALL_FOR_LINK_EXCEPT_HIDE);
                        } else {
                            fileList = itemList;
                            isNull = true;
                            mHandler.sendEmptyMessage(1);
                        }
                    } else if (currentCommand.equals(LS_FILE_ALL_FOR_LINK_EXCEPT_HIDE)) {
                        linkFiles.clear();
                        fileList = itemList;
                        isNull = true;
                        mHandler.sendEmptyMessage(1);
                    } else if (currentCommand.equals(SU)) {
                        if (content.charAt(0) == '0') {
                            //取得了root权限
                            isGetRoot = true;
                            exeCommand(permissionCommand);
                        } else {
                            //未取得root权限
                            isGetRoot = false;
                        }
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取输入流的错误结果
     *
     */
    private void readErrorStream() {
        try {
            String content = "";
            StringBuffer sb = new StringBuffer();
            if (mErrorReader == null) {
                mErrorReader = new InputStreamReader(mErrorStream);
            }
            if (mErrorBr == null) {
                mErrorBr = new BufferedReader(mErrorReader);
            }

            while ((content = mErrorBr.readLine()) != null) {
                System.out.println("command-error->:" + currentCommand + " -error-conten--->:" +content);
                sb.append(content + "\n");
                if (!mErrorBr.ready()){
                    result = sb.toString();
                    sb.setLength(0);
                    mHandler.sendEmptyMessage(-1);
                    if (result.toLowerCase().contains("permission denied")) {
                        if (isGetRoot) {
                            permissionCommand = currentCommand;
                            exeCommand(SU);
                        }
                    } else if (result.toLowerCase().contains("read-only file system")) {

                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class MyHandler extends Handler {
        private WeakReference<ShellUtil> reference;
        private ShellUtil cmdUtil;

        public MyHandler(ShellUtil cUtil) {
            reference = new WeakReference<ShellUtil>(cUtil);
            cmdUtil = reference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //System.out.println("result--->" + cmdUtil.result);
                    if (cmdUtil.resultListener != null) {
                        cmdUtil.resultListener.onLoadComplet(cmdUtil.fileList);
                    }
                    break;

                case -1:
                    //System.out.println("error result--->" + cmdUtil.result);
                    if (cmdUtil.resultListener != null) {
                        cmdUtil.resultListener.onError(cmdUtil.result);
                    }
                    break;
            }
        }
    }

    static class ResultThread implements Runnable {
        private int type;
        private WeakReference<ShellUtil> wReference;

        public ResultThread(ShellUtil cUtil, int type) {
            this.type = type;
            wReference = new WeakReference<ShellUtil>(cUtil);
        }

        @Override
        public void run() {
            ShellUtil cu = wReference.get();
            if (cu != null) {
                if (type == 1)
                    cu.readStream();
                else
                    cu.readErrorStream();
            }
        }
    }

    public void setResultListener(OnResultListener resultListener) {
        this.resultListener = resultListener;
    }

    public interface OnResultListener {
        void onLoadComplet(List<FileItem> list);

        void onError(String msg);
    }

    public void release() {
        try {
            if (mBr != null) {
                mBr.close();
            }

            if (mReader != null) {
                mReader.close();
            }

            if (mInStream != null) {
                mInStream.close();
            }

            if (mOutStream != null) {
                mOutStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mProcess.destroy();
            mBr = null;
            mReader = null;
            mInStream = null;
            mOutStream = null;
            instance = null;
        }
    }
}
