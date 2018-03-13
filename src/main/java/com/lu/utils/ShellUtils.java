package com.lu.utils;

import com.alibaba.fastjson.JSON;
import com.lu.App;
import com.lu.model.FileItem;
import com.lu.model.TempItem;
import com.lu.shell.ShellTermSession;
import com.lu.shell.TermSession;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by bulefin on 2018/2/26.
 */

public class ShellUtils {

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

    private int mIndex = -6;
    private boolean isRoot;
    private boolean isMountCmd;
    private TempItem mTempItem;

    private static volatile ShellUtils instance;
    private OnResultListener mResultListener;

    private static TermSession mTermSession;

    /**
     * 单例模式
     * @return
     */
    public static final ShellUtils get() {
        if (instance == null) {
            synchronized (ShellUtils.class) {
                if (instance == null) {
                    instance = new ShellUtils();
                    instance.initSession();
                }
            }
        }
        return instance;
    }

    private boolean initSession() {
        try {
            mTermSession  = new ShellTermSession();
            mTermSession.setContentListener(mContentListener);
            mTermSession.initialize();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    /**
     * 执行用户输入的命令
     *
     * @param command
     */
    public void exeCommand(String command) {
        if (mTermSession != null) {
            System.out.println("exeCommand--------用户输入的命令------------------->" + command);
            mTermSession.write(command + "\n");
        }
    }

    //static Pattern patternJson = Pattern.compile("\\{\".+\"\\}(\\r|\\n)*");
    //   \{"([^"]|\\")+":"([^"]|\\")+"(,"([^"]|\\")+":"([^"]|\\")+")*\}$
    //
    static Pattern patternJson = Pattern.compile("\\{\"([^\"]|\\\\\")+\":\"([^\"]|\\\\\")+\"(,\"([^\"]|\\\\\")+\":\"([^\"]|\\\\\")+\")*\\}$");
    private void parseContent(String content) {
        //System.out.println("parse------------>" + content);
        try {
            if (patternJson.matcher(content).matches()) {
                FileItem item = JSON.parseObject(content, FileItem.class);
                switch (item.flag){
                    case "f":
                        if (item.error != null) {
                            if (item.error.toLowerCase().contains("permission denied") && !isRoot) {
                                mTempItem = JSON.parseObject(content, TempItem.class);
                                exeCommand("su");
                            }
                        } else {
                            mResultListener.onLsLoad(item);
                        }
                        break;
                    case "s":
                        mResultListener.onSizeComplete(item.id, content);
                        break;
                    case "cp":
                        mResultListener.onCpAction(item.id, content);
                        break;
                    case "mv":
                        mResultListener.onMvAction(item.id, content);
                        break;
                    case "del":
                        mResultListener.onDelAction(item.id, content);
                        break;
                    case "nd":
                        mResultListener.onCreateDirComplete(item.id, content);
                        break;
                    case "nf":
                        mResultListener.onCreateFileComplete(item.id, content);
                        break;
                    case "rn":
                        mResultListener.onRenameComplete(item.id, content);
                        break;
                    case "realp":
                        mResultListener.onRealPath(item.id, item.getPath());
                        break;
                    case "mou":
                        //System.out.println("-mou-->" + content);
                        mResultListener.onMountEvent(item.id, false, item.state);
                        break;
                    case "chm":
                        mResultListener.onCHMAction(item.id, content);
                        break;
                    case "ltext":
                        mResultListener.onTextAction(item.id, content);
                        break;
                    case "etext":
                        mResultListener.onTextEdit(content);
                        break;
                    case "uid":
                        isRoot = "0".equals(JSON.parseObject(content).getString("uid"));
                        break;
                    default:
                        System.out.println("default content--->" + content);
                        break;
                }
            } else {
                System.out.println("not matches()----------->" + content);
                if (isMountCmd) {
                    if (PermissionUtils.parseSys(content)){
                        isMountCmd = false;
                        mResultListener.onMountEvent(PermissionUtils.index, true, false);
                    }
                }
                if (content.endsWith("mount") && content.length() > 8) {
                    isMountCmd = true;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void parseTempItem(TempItem item) {
        if (item == null) {
            return;
        }
        FileUtils fileUtils = FileUtils.get();
        switch (item.flag) {
            case "f":
                fileUtils.listAllFile(item.id, item.path);
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
                //mHandler.obtainMessage(FLAG_REQGETROOT, item).sendToTarget();
                break;
            case "ltext":
                fileUtils.do_text(item.id, item.path, App.tempFilePath, 'l');
                break;
            case "etext":
                fileUtils.do_text(item.id, item.path, App.tempFilePath, 'e');
                break;
            default:
                System.out.println("default content--->" + item.content);
                break;
        }

    }

   private TermSession.OnContentListener mContentListener = new TermSession.OnContentListener() {
       @Override
       public void onContent(String content) {
           parseContent(content.trim());
       }

       @Override
       public void onRequestRoot(String str) {
           str = str.trim();
           if (str.charAt(str.length() - 1) == '#') {
               isRoot = true;
               parseTempItem(mTempItem);
               mTempItem = null;
           } else {
               ToastUitls.showLMsgAtCenter("无法获取Root权限");
           }
           mResultListener.onRequestRoot(mIndex, isRoot);
       }
   };

    public void setResultListener(OnResultListener resultListener) {
        this.mResultListener = resultListener;
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
        void onTextEdit(String str);
        void onRealPath(int index, String realPath);
        void onMountEvent(int index, boolean isCheck, boolean success);
        void onRequestRoot(int index, boolean success);
        void onError(String msg);
    }

}
