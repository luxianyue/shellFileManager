package com.lu.model;

import com.lu.utils.FileUtils;

/**
 * Created by bulefin on 2017/11/8.
 */

public class Path {
    private String path;
    private String name;

    public Path() {}

    public Path(String path) {
        this.path = path;
    }

    public Path(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public void set(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
