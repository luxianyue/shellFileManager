package com.lu.model;

import android.widget.TextView;

import com.lu.App;
import com.lu.filemanager2.R;
import com.lu.utils.FileUtils;

/**
 * Created by lu on 2016/10/29.
 */

public class FileItem {
    //fragment index
    public int id;
    //date and time
    public long dt;
    //link to
    public String lt;
    //current path
    public String lsp;
    //type and permission of file
    public String tp;
    //group of file
    public String g;
    //name of file
    public String n;
    //path of file
    public String p;
    //user of file
    public String u;
    //size of file
    public long s;
    //numbers of file in folder
    public long ct;

    public String flag;
    public String error;
    public boolean state;
    public boolean isOver;

    //文件类型
    private int type;

    //文件显示的图标
    private int icon;

    private String major;

    private String minor;

    public FileItem(){}

    public FileItem(boolean isUpper){
        this.isUpper = isUpper;
        this.n = "..";
        this.tp = "d0rwxrwxrwx";
    }

    /**
     * 文件是否被选中
     */
    private boolean isCheck;

    /**
     * 是否是文件操作模式, copy,cut,move,delete
     */
    private boolean isOpera;

    /**
     * 上层文件夹的标志
     */
    public boolean isUpper;

    public String getName() {
        return n;
    }

    /**
     * 是否是文件夹
     */
    public boolean isFolder() {
        return isLink() ? isLinkPath() : (tp != null ? tp.charAt(0) == 'd' : false);
    }

    /**
     * 是否是隐藏的文件
     */
    public boolean isHidden() {
        return n != null ? n.charAt(0) == '.' : false;
    }


    public boolean isLink() {
        return tp != null ? tp.charAt(0) == 'l' : false;
    }

    /**
     * 链接文件是否链接到目录
     */
    public boolean isLinkPath() {
        return tp != null ? tp.charAt(1) == 'd' : false;
    }

    /**
     * 链接文件所链接的路径
     */
    public String linkTo() {
        return lt;
    }

    /**
     * 文件的大小
     */
    public long size() {
        return isFolder() ? 0 : s;
    }

    public String formatSize() {
        return isUpper ? App.context().getString(R.string.upper_dir) : (isFolder() ? count() + App.context().getString(R.string.term) : FileUtils.getFormatByte(s));
    }

    /**
     * 文件最后修改的时间 dt 为秒级别
     */
    public long lastModified() {
        return dt * 1000;
    }

    public String getCurPath() {
        return lsp;
    }

    /**
     * 文件的绝对路径
     */
    public String getPath() {
        return p;
    }

    /**
     * 文件的权限
     */
    public String getPer() {
        return tp.length() > 2 ? tp.substring(2) : "";
    }

    /**
     * 文件的所有者
     */
    public String getUser() {
        return u;
    }

    /**
     * 文件所有者所在的组
     */
    public String getGroup() {
        return g;
    }

    public String getMajor() {
        return major;
    }

    public String getMinor() {
        return minor;
    }

    /**
     * 文件夹里所包含的文件数量
     */
    public long count() {
        return ct;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public boolean isOpera() {
        return isOpera;
    }

    public void setOpera(boolean opera) {
        isOpera = opera;
    }

    public void setCheck(boolean check) {
        isCheck = isUpper ? false : check;
    }

    public int getType() {
        return type;
    }

    public TextView tvPermission;

    private int position;

    public void setPosition(int position) {
        this.position = position;
    }

    public int position() {
        return position;
    }
}
