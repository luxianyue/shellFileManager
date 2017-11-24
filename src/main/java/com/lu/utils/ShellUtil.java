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
import com.lu.model.Item;

public class ShellUtil {

    private static final int
            FLAG_F = 101,
            FLAG_S = 102,
            FLAG_CP = 103,
            FLAG_MV = 104,
            FLAG_DEL = 105,
            FLAG_RN = 106,
            FLAG_ND = 107,
            FLAG_NF = 108,
            FLAG_CHM = 109,
            FLAG_REQGETROOT = 110,
            FLAG_DEFAULT = 111;

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

    private String suCommand;
    private String curtCommand;
    private String permissionCommand;

    private static boolean isGetRoot;

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

    static Pattern patternJson = Pattern.compile("\\{'[^']+':'[^']+'[ ]*(,[ ]*'[^']+':'[^']+'[ ]*)*\\}");

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
            exeCommand(App.tools + " -uid");
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
    public static final ShellUtil get() {
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
                //commandQueue.offer(command);
                System.out.println("exeCommand--------用户输入的命令------------------->" + command);
                mOutStream.write((command + "\n").getBytes());
                if (command.startsWith("su")) {
                    exeCommand("echo $?");
                }
            }
        } catch (IOException e) {
            System.out.println("error execommand " + e);
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
            Matcher jsonMatcher = null;

            List<FileItem> itemList = null;
            FileItem item;
            String currentPath = null;
            String command = null;
            while ((content = mBr.readLine()) != null) {
                //System.out.println("curtCommand-------->" + curtCommand + ",  content--->" + content);
                jsonMatcher = patternJson.matcher(content);
                if (jsonMatcher.matches()) {
                    item = JSON.parseObject(content, FileItem.class);
                    switch (item.flag){
                        case "f":
                            if (item.isOver) {
                                fileList = itemList;
                                itemList = null;
                                mHandler.sendEmptyMessage(FLAG_F);
                                break;
                            }
                            if (itemList == null) {
                                itemList = new ArrayList<>();
                            }
                            itemList.add(item);
                            break;
                        case "s":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_S, content).sendToTarget();
                                break;
                            }
                            break;
                        case "cp":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_CP, content).sendToTarget();
                            }
                            //mHandler.obtainMessage(FLAG_CP, content).sendToTarget();
                            //System.out.println("cp-->" + content);
                            break;
                        case "mv":
                            /*if (item.isOver) {
                                mHandler.sendEmptyMessage(FLAG_MV);
                                break;
                            }*/
                            mHandler.obtainMessage(FLAG_MV, content).sendToTarget();
                            System.out.println("mv-->" + content);
                            break;
                        case "del":
                            /*if (item.isOver) {
                                mHandler.sendEmptyMessage(FLAG_DEL);
                                break;
                            }*/
                            mHandler.obtainMessage(FLAG_DEL, content).sendToTarget();
                            System.out.println("del-->" + content);
                            break;
                        case "nd":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_ND, content).sendToTarget();
                                break;
                            }
                            break;
                        case "nf":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_NF, content).sendToTarget();
                                break;
                            }
                            break;
                        case "rn":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_RN, content).sendToTarget();
                                break;
                            }
                            break;
                        case "su":
                            System.out.println("su content-->" + content);
                            Item itm = JSON.parseObject(content, Item.class);
                            if ("0".equals(itm.content)) {
                                paseItem(itm);
                            } else {
                                ToastUitls.showLMsgAtCenter("not permission");
                            }
                            break;
                        case "chm":
                            System.out.println("chm content-->" + content);
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_CHM, content).sendToTarget();
                            }
                            break;
                        case "uid":
                            if (item.isOver) {
                                if ("0".equals(JSON.parseObject(content).getString("uid"))) {
                                    isGetRoot = true;
                                } else {
                                    isGetRoot = false;
                                }
                            }
                            break;
                        default:
                            System.out.println("default content--->" + content);
                            break;
                    }
                } else {
                    System.out.println("jsonMatcher not matches()-------->" + curtCommand + ",  content--->" + content);
                }

                //System.out.println("=========================================");
                if (!mBr.ready()) {
                    System.out.println(curtCommand +" !mBr.ready()=======>" + !mBr.ready() + "--->" + content);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getCause());
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

            Item item = null;
            Matcher errorMatcher = null;
            while ((content = mErrorBr.readLine()) != null) {
                System.out.println("command-error-> -error-conten--->:" +content);
                errorMatcher = patternJson.matcher(content);
                if (errorMatcher.matches()) {
                    item = JSON.parseObject(content, Item.class);
                    switch (item.flag){
                        case "f":
                            if (item.error.toLowerCase().contains("permission denied")) {
                                exeCommand(SU + "\necho \"{'flag':'su','fg':'f','path':'" + item.path + "','content':'$?'}\"");
                            }
                            break;
                        case "s":
                            break;
                        case "cp":
                            break;
                        case "mv":
                            break;
                        case "del":
                            break;
                        case "nd":
                            break;
                        case "nf":
                            break;
                        case "rn":
                            break;
                        case "su":
                            break;
                        default:
                            System.out.println("default error content--->" + content);
                            break;
                    }
                } else {
                    System.out.println("jsonMatcher not matches()-------->"  + ",  content--->" + content);
                }
                if (!mErrorBr.ready()){
                    mHandler.sendEmptyMessage(-1);
                    /*if (content.toLowerCase().contains("permission denied")) {
                        if (isGetRoot) {
                            permissionCommand = curtCommand;
                            curtCommand = null;
                            exeCommand(SU + "\necho \"{'flag':'su','content':'$?'}\"");
                        }
                    } else if (content.toLowerCase().contains("read-only file system")) {

                    } else {
                        curtCommand = null;
                    }*/

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getCause());
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
                case FLAG_F:
                    //System.out.println("result--->" + cmdUtil.result);
                    cmdUtil.resultListener.onLoadComplete(cmdUtil.fileList);
                    break;
                case FLAG_S:
                    cmdUtil.resultListener.onSizeComplete(msg.obj.toString());
                    break;
                case FLAG_RN:
                    cmdUtil.resultListener.onRenameComplete(msg.obj.toString());
                    break;
                case FLAG_ND:
                    cmdUtil.resultListener.onCreateDirComplete(msg.obj.toString());
                    break;
                case FLAG_NF:
                    cmdUtil.resultListener.onCreateFileComplete(msg.obj.toString());
                    break;
                case FLAG_CP:
                    /*JSONObject cpJson = JSON.parseObject(msg.obj.toString());
                    if (cpJson.getBooleanValue("isOver")) {
                    } else {

                    }*/
                    cmdUtil.resultListener.onCpAction(msg.obj.toString());
                    System.out.println("---onCpAction");
                    break;
                case FLAG_MV:
                    System.out.println("---onMvComplete");
                    cmdUtil.resultListener.onMvComplete();
                    break;
                case FLAG_DEL:
                    System.out.println("---onDelComplete");
                    cmdUtil.resultListener.onDelComplete();
                    break;
                case FLAG_CHM:
                    cmdUtil.resultListener.onCHMAction(msg.obj.toString());
                    break;
                case FLAG_REQGETROOT:
                    cmdUtil.resultListener.onReqGetRoot((Item) msg.obj);
                    break;
                case FLAG_DEFAULT:
                    break;
                case -1:
                    //System.out.println("error result--->" + cmdUtil.result);
                    cmdUtil.resultListener.onError(cmdUtil.result);
                    break;
            }
        }
    }

    private void paseItem(Item item) {
        FileUtil fileUtil = FileUtil.getInstance();
        switch (item.fg) {
            case "f":
                fileUtil.listAllFile(item.path);
                break;
            case "s":
                break;
            case "cp":
                break;
            case "mv":
                break;
            case "del":
                break;
            case "nd":
                break;
            case "nf":
                break;
            case "rn":
                break;
            case "per":
                mHandler.obtainMessage(FLAG_REQGETROOT, item).sendToTarget();
                break;
            default:
                System.out.println("default content--->" + item.content);
                break;
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
        void onSizeComplete(String str);
        void onRenameComplete(String str);
        void onCreateDirComplete(String str);
        void onCreateFileComplete(String str);
        void onCpAction(String str);
        void onMvComplete();
        void onDelComplete();
        void onCHMAction(String str);
        void onReqGetRoot(Item item);
        void onError(String msg);
    }

    public static boolean isGetRoot() {
        return isGetRoot;
    }

    public void release() {
        isGetRoot = false;
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
