package com.lu.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;

import com.lu.fragment.ContentFragment;

import java.util.List;

/**
 * Created by lu on 2016/10/23.
 */

public class FragmentAdapter extends FragmentStatePagerAdapter {
    private FragmentManager mFragmentManager;

    private int destroyIndex = -1;
    private List<Fragment> mFragments;
    private List<String> titleList;
    public FragmentAdapter(List<Fragment> mList,List<String> titleList, FragmentManager fm) {
        super(fm);
        this.mFragments = mList;
        this.titleList = titleList;
        mFragmentManager = fm;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = getItem(position);
        if (fragment.isAdded()) {
            return fragment;
        }
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(container.getId(), fragment).commitNowAllowingStateLoss();
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    }

    @Override
    public int getCount() {
        return mFragments == null ? 0 : mFragments.size();
    }

    public void setDestroyIndex(int destroyIndex) {
        this.destroyIndex = destroyIndex;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titleList.get(position);
    }
}
