package com.lu.model;

import com.lu.filemanager2.R;

/**
 * Created by lu on 2016/10/29.
 */

public class FileItem {
    //date and time
    public String dt;
    //link to
    public String lt;
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

    //文件类型
    private int type;

    //文件显示的图标
    private int icon;

    private String major;

    private String minor;

    /**
     * 文件是否被选中
     */
    private boolean isCheck;

    /**
     * 是否是文件操作模式, copy,cut,move,delete
     */
    private boolean isOpera;


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
    public String getLinkTo() {
        return lt;
    }

    public int getIcon() {
        return isFolder() ? R.drawable.folder_blue : R.drawable.unknown;
    }

    /**
     * 文件的大小
     */
    public long getSize() {
        return s;
    }

    /**
     * 文件最后修改的日期
     */
    public String getDate() {
        return dt != null ? dt.split("\\s+")[0] : null;
    }

    /**
     * 文件最后修改的时间
     */
    public String getTime() {
        return dt != null ? dt.split("\\s+")[1] : null;
    }

    /**
     * 文件的绝对路径
     */
    public String getPath() {
        return p;
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
    public long getCount() {
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
        isCheck = check;
    }

    public int getType() {
        return type;
    }
}
