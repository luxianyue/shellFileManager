package com.lu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created by lu on 2016/10/29.
 */

public abstract class BasedAdapter<T> extends BaseAdapter {
    protected List<T> mList;
    protected Context context;
    protected LayoutInflater mLayoutInflater;

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return mList != null ? mList.get(position) : null;
    }

    public List<T> getList() {
        return mList;
    }

    public void setList(List<T> mList) {
        beforeSetList();
        this.mList = mList;
        notifyDataSetChanged();
    }

    public abstract void beforeSetList();
}
