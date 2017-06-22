package com.lu.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by lu on 2016/10/23.
 */

public class FragmentAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> mList;
    private List<String> titleList;
    public FragmentAdapter(List<Fragment> mList,List<String> titleList, FragmentManager fm) {
        super(fm);
        this.mList = mList;
        this.titleList = titleList;
    }

    @Override
    public Fragment getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titleList.get(position);
    }
}
