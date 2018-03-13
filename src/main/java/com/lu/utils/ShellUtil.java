package com.lu.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.os.Message;

import com.alibaba.fastjson.JSON;
import com.lu.App;
import com.lu.model.TempItem;
import com.lu.model.FileItem;

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
            FLAG_L_TEXT = 110,
            FLAG_E_TEXT = 111,
            FLAG_REQGETROOT = 112,
            FLAG_DEFAULT = 113;

    private static final String
            MY_LS = "tools",
            LS_FILE_ALL = "ls -al",
            LS_FILE_EXCEPT_HIDE = "ls -l",
            LS_FILE_ALL_FOR_LINK = "ls -aF",
            LS_FILE_ALL_FOR_LINK_EXCEPT_HIDE = "ls -F",
            CD = "cd",
            SU = "su",
            PWD = "pwd",
            SHELL = "sh";

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
    private onTextActivityListener onTextActivityListener;

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

    static Pattern patternJson = Pattern.compile("\\{\".+\"\\}");

    Matcher matcher;

    /**
     * 缓冲字节数组的大小  2*1024
     */
    private static final int BUF_SIZE = 1 << 10;

    /**
     * 用来接收命令执行后的(输出)结果
     */
    private String result;

    private Queue<String> commandQueue;
    private Queue<String> errorQueue;

    private Map<Integer, List<FileItem>> mFileItemListMap;


    private ShellUtil() {
        mHandler = new MyHandler(this);
        commandQueue = new LinkedList<>();
        errorQueue = new LinkedList<>();
        //把正常的结果输出流和错误流合并在同一个流里面
        //processBuilder.redirectErrorStream(true);
        try {
            //申请获取终端
            mProcess = new ProcessBuilder(SHELL).directory(new File(App.exePath)).start();
            mInStream = mProcess.getInputStream();
            mErrorStream = mProcess.getErrorStream();
            mOutStream = mProcess.getOutputStream();
            //开启线程用来处理命令执行后的相关信息，正常结果和错误结果
            startNormalStreamThread();
            startErrorStreamThread();
            exeCommand(App.tools + " -uid");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void startNormalStreamThread() {
        new Thread(new ResultThread(this, 1)).start();
    }

    private void startErrorStreamThread() {
        new Thread(new ResultThread(this, 2)).start();
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

   private Map<Integer, List<FileItem>> getItemListMap() {
        if (mFileItemListMap == null) {
            mFileItemListMap = new HashMap<>();
        }
        return mFileItemListMap;
   }

   public void clearItemList(int index) {
        if (getItemListMap().get(index) != null) {
            getItemListMap().get(index).clear();
        }
    }

    public void removeItemList(int index) {
        if (getItemListMap().get(index) != null) {
            getItemListMap().remove(index);
        }
    }

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
                /*if (command.startsWith("su")) {
                    exeCommand("echo $?");
                }*/
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
            byte mBuffer[] = new byte[1024];
            while(true) {
                int read = mInStream.read(mBuffer);
                System.out.println("read----->" + new String(mBuffer,0,read));
            }
           /* while ((content = mBr.readLine()) != null) {
                jsonMatcher = patternJson.matcher(content);
                if (jsonMatcher.matches()) {
                    item = JSON.parseObject(content, FileItem.class);
                    switch (item.flag){
                        case "f":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_F, item.id, 0).sendToTarget();
                                break;
                            }
                            if ((itemList = getItemListMap().get(item.id)) == null) {
                                getItemListMap().put(item.id, new ArrayList<FileItem>());
                                itemList = getItemListMap().get(item.id);
                            }

                            itemList.add(item);
                            break;
                        case "s":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_S, item.id, 0, content).sendToTarget();
                                break;
                            }
                            break;
                        case "cp":
                            mHandler.obtainMessage(FLAG_CP,item.id, 0, content).sendToTarget();
                            System.out.println("shelluitl---cp-->" + content);
                            break;
                        case "mv":
                            mHandler.obtainMessage(FLAG_MV,item.id, 0, content).sendToTarget();
                            System.out.println("mv-->" + content);
                            break;
                        case "del":
                            mHandler.obtainMessage(FLAG_DEL,item.id, 0, content).sendToTarget();
                            System.out.println("del-->" + content);
                            break;
                        case "nd":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_ND,item.id, 0, content).sendToTarget();
                                break;
                            }
                            break;
                        case "nf":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_NF,item.id, 0, content).sendToTarget();
                                break;
                            }
                            break;
                        case "rn":
                            if (item.isOver) {
                                mHandler.obtainMessage(FLAG_RN,item.id, 0, content).sendToTarget();
                                break;
                            }
                            break;
                        case "su":
                            System.out.println("su content-->" + content);
                            TempItem itm = JSON.parseObject(content, TempItem.class);
                            if ("0".equals(itm.content)) {
                                isGetRoot = true;
                                paseItem(itm);
                            } else {
                                //ToastUitls.showLMsgAtCenter("not permission");
                            }
                            break;
                        case "mount":
                            break;
                        case "chm":
                            System.out.println("chm content-->" + content);
                            mHandler.obtainMessage(FLAG_CHM,item.id, 0, content).sendToTarget();
                            break;
                        case "ltext":
                        case "etext":
                            if (item.isOver) {
                                mHandler.obtainMessage("ltext".equals(item.flag) ? FLAG_L_TEXT : FLAG_E_TEXT,item.id, 0, content).sendToTarget();
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
            }*/

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("readStream exception------>"+ e.toString());
            startNormalStreamThread();
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

            TempItem TempItem = null;
            Matcher errorMatcher = null;
            while ((content = mErrorBr.readLine()) != null) {
                System.out.println("command-error-> -error-conten--->:" +content);
                errorMatcher = patternJson.matcher(content);
                if (errorMatcher.matches()) {
                    TempItem = JSON.parseObject(content, TempItem.class);
                    switch (TempItem.flag){
                        case "f":
                            System.out.println("error fg=f ====>" + TempItem.error);
                            if (TempItem.error.toLowerCase().contains("permission denied")) {
                                exeCommand(SU + "\necho \"{\\\"id\\\":\\\"" + TempItem.id + "\\\",\\\"flag\\\":\\\"su\\\",\\\"fg\\\":\\\"f\\\",\\\"path\\\":\\\"" + TempItem.path + "\\\",\\\"content\\\":\\\"$?\\\"}\"");
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
                        case "chm":
                            //{"flag":"chm","error":"Read-only file system","mode":"0757","path":"/system"}
                            if ("Read-only file system".equalsIgnoreCase(TempItem.error)) {
                                Object chmObj[] = PermissionUtils.isOnlyReadFileSys(TempItem.path);
                                System.out.println("mount remount-->" + chmObj[1] + "  " + chmObj[2]);
                                exeCommand(MOUNT_RW + chmObj[1] + " " + chmObj[2] + "\necho \"{\\\"id\\\":\\\"" + TempItem.id + "\\\",\\\"flag\\\":\\\"mount\\\",\\\"fg\\\":\\\"chm\\\",\\\"path\\\":\\\"" + TempItem.path + "\\\",\\\"content\\\":\\\"$?\\\"}\"");
                            }
                            break;
                        case "ltext":
                        case "etext":
                            if (TempItem.error.toLowerCase().contains("permission denied")) {
                                exeCommand(SU + "\necho \"{\\\"id:\\\"" + TempItem.id + "\\\",\\\"flag\\\":\\\"su\\\",\\\"fg\\\":\\\""+ TempItem.fg +"\\\",\\\"path\\\":\\\"" + TempItem.path + "\\\",\\\"content\\\":\\\"$?\\\"}\"");
                            }
                            break;
                        case "su":
                            break;
                        default:
                            System.out.println("default error content--->" + content);
                            break;
                    }
                } else {
                    System.out.println("error jsonMatcher not matches()-------->"  + ",  content--->" + content);
                }
                if (!mErrorBr.ready()){
                    mHandler.sendEmptyMessage(-1);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            startErrorStreamThread();
            System.out.println("error  catch (Exception e)-->" + e.toString());
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
                    List<FileItem> fList = cmdUtil.getItemListMap().get(msg.arg1);
                    if (fList == null) {
                        fList = new ArrayList<>();
                        cmdUtil.getItemListMap().put(msg.arg1, fList);
                    }
                    //cmdUtil.resultListener.onLsLoad(msg.arg1, fList);
                    break;
                case FLAG_S:
                    cmdUtil.resultListener.onSizeComplete(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_RN:
                    cmdUtil.resultListener.onRenameComplete(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_ND:
                    cmdUtil.resultListener.onCreateDirComplete(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_NF:
                    cmdUtil.resultListener.onCreateFileComplete(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_CP:
                    /*JSONObject cpJson = JSON.parseObject(msg.obj.toString());
                    if (cpJson.getBooleanValue("isOver")) {
                    } else {

                    }*/
                    cmdUtil.resultListener.onCpAction(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_MV:
                    cmdUtil.resultListener.onMvAction(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_DEL:
                    cmdUtil.resultListener.onDelAction(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_CHM:
                    cmdUtil.resultListener.onCHMAction(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_E_TEXT:
                    if (cmdUtil.onTextActivityListener != null) {
                        cmdUtil.onTextActivityListener.onTextSaveAction(msg.obj.toString());
                    }
                    break;
                case FLAG_L_TEXT:
                    cmdUtil.resultListener.onTextAction(msg.arg1, msg.obj.toString());
                    break;
                case FLAG_REQGETROOT:
                    cmdUtil.resultListener.onReqGetRoot((TempItem) msg.obj);
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

    private void paseItem(TempItem TempItem) {
        FileUtils fileUtils = FileUtils.get();
        switch (TempItem.fg) {
            case "f":
                fileUtils.listAllFile(TempItem.id, TempItem.path);
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
                mHandler.obtainMessage(FLAG_REQGETROOT, TempItem).sendToTarget();
                break;
            case "ltext":
                fileUtils.do_text(TempItem.id, TempItem.path, App.tempFilePath, 'l');
                break;
            case "etext":
                fileUtils.do_text(TempItem.id, TempItem.path, App.tempFilePath, 'e');
                break;
            default:
                System.out.println("default content--->" + TempItem.content);
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

    public void setTextActivityListener(ShellUtil.onTextActivityListener onTextActivityListener) {
        this.onTextActivityListener = onTextActivityListener;
    }

    public interface OnResultListener {
        void onLsLoad(FileItem item);
        void onSizeComplete(int index, String str);
        void onRenameComplete(int index, String str);
        void onCreateDirComplete(int index, String str);
        void onCreateFileComplete(int index, String str);
        void onCpAction(int index, String str);
        void onMvAction(int index, String str);
        void onDelAction(int index, String str);
        void onCHMAction(int index, String str);
        void onTextAction(int index, String str);
        void onReqGetRoot(TempItem TempItem);
        void onError(String msg);
    }

    public interface onTextActivityListener{
        void onTextSaveAction(String str);
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
        System.out.println("shell release-->" + instance);
    }
}
