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

import com.alibaba.fastjson.JSON;
import com.lu.App;
import com.lu.model.FileItem;

public class ShellUtil {

    private static final String
            MY_LS = "tools",
            LS_FILE_ALL = "ls -al",
            LS_FILE_EXCEPT_HIDE = "ls -l",
            LS_FILE_ALL_FOR_LINK = "ls -aF",
            LS_FILE_ALL_FOR_LINK_EXCEPT_HIDE = "ls -F",
            CD = "cd",
            SU = "su",
            PWD = "pwd",
            SHELL = "/system/bin/sh";

    public static final String MOUNT_RW = "mount -o remount,rw ";
    public static final String MOUNT_RO = "mount -o remount,ro ";

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

    private String curtCommand;
    private String permissionCommand;

    private boolean isGetRoot = true;

    //当前shell进程pid
    private int shellPid;


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
    private Queue<String> errorQueue;


    private ShellUtil() {
        mHandler = new MyHandler(this);
        commandQueue = new LinkedList<>();
        errorQueue = new LinkedList<>();
        //把正常的结果输出流和错误流合并在同一个流里面
        //processBuilder.redirectErrorStream(true);
        try {
            //申请获取终端
            mProcess = new ProcessBuilder(SHELL).start();
            mInStream = mProcess.getInputStream();
            mErrorStream = mProcess.getErrorStream();
            mOutStream = mProcess.getOutputStream();
            //exeCommand("echo pid$$");
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
    /*public static final ShellUtil get() {
        return new ShellUtil();
    }*/

    /**
     * 执行用户输入的命令
     *
     * @param command
     */
    public void exeCommand(String command) {
        try {
            if (mOutStream != null) {
                commandQueue.offer(command);
                System.out.println("exeCommand--------------------------->" + command);
                mOutStream.write((command + "\n").getBytes());
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
            FileItem item;
            String currentPath = null;
            String command = null;
            while ((content = mBr.readLine()) != null) {
                if (curtCommand == null) {
                    curtCommand = commandQueue.poll();
                    System.out.println("curtCommand-------->" + curtCommand + ",  content--->" + content);
                }
                //System.out.println("curtCommand-------->" + curtCommand + ",  content--->" + content);
                if (curtCommand.startsWith(App.tools + " -f")) {
                    if (itemList == null) {
                        itemList = new ArrayList<>();
                    }
                    item = JSON.parseObject(content, FileItem.class);
                    itemList.add(item);
                }
                /*if (curtCommand.startsWith(App.tools + " -cp")) {
                    System.out.println("content--->" + content);
                }
                if (curtCommand.startsWith(App.tools + " -mv")) {
                    System.out.println("content--->" + content);
                }
                if (curtCommand.startsWith(App.tools + " -del")) {
                    System.out.println("content--->" + content);
                }*/

                //System.out.println("=========================================");
                if (!mBr.ready()) {
                    if (curtCommand.startsWith(App.tools + " -f")) {
                        curtCommand = null;
                        fileList = itemList;
                        itemList = null;
                        mHandler.sendEmptyMessage(1);
                        continue;
                    }
                    if (curtCommand.startsWith(App.tools + " -s")) {
                        curtCommand = null;
                        mHandler.obtainMessage(2, content).sendToTarget();
                        continue;
                    }
                    if (curtCommand.startsWith(App.tools + " -rn")) {
                        curtCommand = null;
                        mHandler.obtainMessage(3, content).sendToTarget();
                        continue;
                    }
                    if (curtCommand.startsWith(App.tools + " -nd")) {
                        curtCommand = null;
                        mHandler.obtainMessage(4, content).sendToTarget();
                        continue;
                    }
                    if (curtCommand.startsWith(App.tools + " -nf")) {
                        curtCommand = null;
                        mHandler.obtainMessage(5, content).sendToTarget();
                        continue;
                    }
                    if (curtCommand.startsWith("su\necho $?")) {
                        curtCommand = null;
                        if ("0".equals(content)) {
                            exeCommand(errorQueue.poll());
                        } else {
                            errorQueue.poll();
                        }
                        continue;
                    }
                    curtCommand = null;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
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
                if (curtCommand == null) {
                    curtCommand = commandQueue.poll();
                    errorQueue.offer(curtCommand);
                }
                System.out.println("command-error->:" + curtCommand + " -error-conten--->:" +content);
                sb.append(content + "\n");
                if (!mErrorBr.ready()){
                    result = sb.toString();
                    sb.setLength(0);
                    mHandler.sendEmptyMessage(-1);
                    if (result.toLowerCase().contains("permission denied")) {
                        if (isGetRoot) {
                            permissionCommand = curtCommand;
                            curtCommand = null;
                            exeCommand(SU + "\necho $?");
                        }
                    } else if (result.toLowerCase().contains("read-only file system")) {

                    } else {
                        curtCommand = null;
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
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
            if (cmdUtil.resultListener == null) {
                return;
            }
            switch (msg.what) {
                case 1:
                    //System.out.println("result--->" + cmdUtil.result);
                    cmdUtil.resultListener.onLoadComplete(cmdUtil.fileList);
                    break;
                case 2:
                    cmdUtil.resultListener.onLoadComplete(msg.obj.toString());
                    break;
                case 3:
                    cmdUtil.resultListener.onRenameComplete(msg.obj.toString());
                    break;
                case 4:
                    cmdUtil.resultListener.onCreateDirComplete(msg.obj.toString());
                    break;
                case 5:
                    cmdUtil.resultListener.onCreateFileComplete(msg.obj.toString());
                    break;
                case -1:
                    //System.out.println("error result--->" + cmdUtil.result);
                    cmdUtil.resultListener.onError(cmdUtil.result);
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
        void onLoadComplete(List<FileItem> list);
        void onLoadComplete(String str);
        void onRenameComplete(String str);
        void onCreateDirComplete(String str);
        void onCreateFileComplete(String str);
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
