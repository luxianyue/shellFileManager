package com.lu.model;

/**
 * Created by lu on 2016/10/29.
 */

public class FileItem {

    //文件类型
    private int type;

    //文件显示的图标
    private int icon;

    private String mode;

    private String user;

    private String group;

    private String major;

    private String minor;

    //文件夹里的文件数量
    private long count;
    /**
     * 文件名
     */
    private String name;

    /**
     * 文件最后被修改的日期
     */
    private String date;

    /**
     * 文件最后被修改的时间
     */
    private String time;

    /**
     * 文件的绝对路径
     */
    private String path;

    /**
     * 文件的大小
     */
    private String size;

    /**
     * 文件是否被选中
     */
    private boolean isCheck;

    /**
     * 是否是文件夹
     */
    private boolean isFolder;

    /**
     * 是否隐藏文件
     */
    private boolean isHidden;

    /**
     * 是否是链接文件
     */
    private boolean isLink;

    /**
     * 链接文件是否链接到目录
     */
    private boolean isLinkPath;

    /**
     * 链接文件链接的路径
     */
    private String linkTo;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isLink() {
        return isLink;
    }

    public void setLink(boolean link) {
        isLink = link;
    }

    public boolean isLinkPath() {
        return isLinkPath;
    }

    public void setLinkPath(boolean linkPath) {
        isLinkPath = linkPath;
    }

    public void setLinkTo(String linkTo) {
        this.linkTo = linkTo;
    }

    public String getLinkTo() {
        return linkTo;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getIcon() {
        return icon;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSize() {
        return size;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
