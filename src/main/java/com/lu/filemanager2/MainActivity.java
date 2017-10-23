package com.lu.filemanager2;

import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.lu.App;
import com.lu.adapter.FragmentAdapter;
import com.lu.activity.BasedActivity;
import com.lu.fragment.ContentFragment;
import com.lu.model.FileItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends BasedActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private LinearLayout mLinearLayoutFloorMenuBar;
    private LinearLayout mLinearLayoutFileHandleMenu1;
    private LinearLayout mLinearLayoutFileHandleMenu2;

    private PopupWindow mPopupWindowFloorBarMenu;
    private PopupWindow mPopupWindowFloorBarAdd;
    private PopupWindow mPopupWindowFloorBarSort;
    private PopupWindow mPopupWindowFloorBarSearch;

    private int[] mClickLocation;
    private static final int menu = 1;
    private static final int add = 2;

    private boolean isFloorMenu2Mode;

    private Set<FileItem> mCheckItems;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
        setListener();

        App.initMyLs();

        mTabLayout.setupWithViewPager(mViewPager);
        List<Fragment> list = new ArrayList<>();
        list.add(new ContentFragment());
        list.add(new ContentFragment());
        //list.add(new ContentFragment());
        //list.add(new ContentFragment());
        List<String> list2 = new ArrayList<>();
        list2.add("title1");
        list2.add("title2");
        //list2.add("title1");
        //list2.add("title2");
        mViewPager.setAdapter(new FragmentAdapter(list, list2, getSupportFragmentManager()));
    }

    private void initLayout() {
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mLinearLayoutFloorMenuBar = (LinearLayout) findViewById(R.id.linearlayout_floor_menu_bar);
        mLinearLayoutFileHandleMenu1 = (LinearLayout) findViewById(R.id.linearlayout_filehandle1);
        mLinearLayoutFileHandleMenu2 = (LinearLayout) findViewById(R.id.linearlayout_filehandle2);
    }

    private void setListener() {
        findViewById(R.id.floor_menu_add).setOnClickListener(this);
        findViewById(R.id.floor_menu_search).setOnClickListener(this);
        findViewById(R.id.floor_menu_sort).setOnClickListener(this);
        findViewById(R.id.floor_menu_close).setOnClickListener(this);
        findViewById(R.id.floor_menu_refresh).setOnClickListener(this);
        findViewById(R.id.floor_menu_menu).setOnClickListener(this);

        findViewById(R.id.image_copy).setOnClickListener(this);
        findViewById(R.id.image_cut).setOnClickListener(this);
        findViewById(R.id.image_delete).setOnClickListener(this);
        findViewById(R.id.image_rename).setOnClickListener(this);

        findViewById(R.id.tv_create).setOnClickListener(this);
        findViewById(R.id.tv_paste).setOnClickListener(this);
        findViewById(R.id.tv_cancle).setOnClickListener(this);

        ((CheckBox)findViewById(R.id.checkbox_handle_menu)).setOnCheckedChangeListener(this);
        findViewById(R.id.image_property).setOnClickListener(this);
        findViewById(R.id.image_menu).setOnClickListener(this);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                //处理菜单键
                onClick(findViewById(R.id.floor_menu_menu));
                return true;
            case KeyEvent.KEYCODE_BACK:
                //处理返回键
                //找到当前显示的fragment
                if (checkBoxIsCheck()) {
                    return true;
                }
                ContentFragment contentFragment = getCurrentShowFragment();
                if (contentFragment.onKeyBack()) {
                    return true;
                }
                if (contentFragment.isItemOpera()) {
                    setShowWhichFoolMenuBar(0);
                    contentFragment.operaItem(false, 0);
                    contentFragment.cancelCheckedItem();
                    return true;
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean checkBoxIsCheck() {
        CheckBox box = (CheckBox) findViewById(R.id.checkbox_handle_menu);
        if (box.isChecked()) {
            box.setChecked(false);
            return true;
        }
        return false;
    }

    private ContentFragment getCurrentShowFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment instanceof ContentFragment) {
                if (((ContentFragment)fragment).isShowToUser()) {
                    return (ContentFragment) fragment;
                }
            }
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floor_menu_add:
                int location[] = getIntArray();
                v.getLocationOnScreen(location);
                if (mPopupWindowFloorBarAdd == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    mPopupWindowFloorBarAdd = initPopupWindow(R.layout.floor_menu_add, (int) (metrics.widthPixels / ((float) 2.3)));
                    mPopupWindowFloorBarAdd.getContentView().findViewById(R.id.floor_menu_add_file).setOnClickListener(this);
                    mPopupWindowFloorBarAdd.getContentView().findViewById(R.id.floor_menu_add_folder).setOnClickListener(this);
                }
                mPopupWindowFloorBarAdd.showAtLocation(v, Gravity.NO_GRAVITY, location[0] + v.getWidth()/2  - 1, location[1] - mPopupWindowFloorBarAdd.getHeight() - 10);
                break;
            case R.id.floor_menu_search:
                int locationSearch[] = getIntArray();
                v.getLocationOnScreen(locationSearch);
                if (mPopupWindowFloorBarSearch == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    mPopupWindowFloorBarSearch = initPopupWindow(R.layout.floor_menu_search, (int) (metrics.widthPixels*1.5 / ((float) 2)));
                }
                mPopupWindowFloorBarSearch.showAtLocation(v, Gravity.CENTER_HORIZONTAL, 0, v.getHeight()/2);
                break;
            case R.id.floor_menu_sort:
                int locationSort[] = getIntArray();
                v.getLocationOnScreen(locationSort);
                if (mPopupWindowFloorBarSort == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    mPopupWindowFloorBarSort = initPopupWindow(R.layout.floor_menu_sort, (int) (metrics.widthPixels*1.5 / ((float) 2)));
                }
                mPopupWindowFloorBarSort.showAtLocation(v, Gravity.CENTER, 0, 0);
                break;
            case R.id.floor_menu_close:
                break;
            case R.id.floor_menu_refresh:
                getCurrentShowFragment().refresh();
                break;
            case R.id.floor_menu_menu:
                int location2[] = getIntArray();
                v.getLocationOnScreen(location2);
                if (mPopupWindowFloorBarMenu == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    mPopupWindowFloorBarMenu = initPopupWindow(R.layout.app_menu, (int) (metrics.widthPixels / ((float) 2.3)));
                    mPopupWindowFloorBarMenu.getContentView().findViewById(R.id.menu_settings).setOnClickListener(this);
                    mPopupWindowFloorBarMenu.getContentView().findViewById(R.id.menu_about).setOnClickListener(this);
                    mPopupWindowFloorBarMenu.getContentView().findViewById(R.id.menu_exit).setOnClickListener(this);
                }
                mPopupWindowFloorBarMenu.showAtLocation(v, Gravity.NO_GRAVITY, location2[0] + v.getWidth() - mPopupWindowFloorBarMenu.getWidth() - 1, location2[1] - mPopupWindowFloorBarMenu.getHeight() - 5);
                break;
            case R.id.image_copy:
                System.out.println("menu1_image_copy");
                mCheckItems = getCurrentShowFragment().getCheckedItem();
                isFloorMenu2Mode = true;
                ((TextView)findViewById(R.id.tv_paste)).setText("复制到此");
                setShowWhichFoolMenuBar(2);
                getCurrentShowFragment().operaItem(true, 0);
                break;
            case R.id.image_cut:
                System.out.println("menu1_image_cut");
                mCheckItems = getCurrentShowFragment().getCheckedItem();
                isFloorMenu2Mode = true;
                ((TextView)findViewById(R.id.tv_paste)).setText("移动到此");
                setShowWhichFoolMenuBar(2);
                getCurrentShowFragment().operaItem(true, 0);
                //getCurrentShowFragment().cancelAllCheckedItem();
                break;
            case R.id.image_delete:
                System.out.println("menu1_image_delete");
                getCurrentShowFragment().del(getCurrentShowFragment().getCheckedItem());
                break;
            case R.id.image_rename:
                System.out.println("menu1_image_rename");
                break;
            case R.id.image_property:
                System.out.println("menu1_image_property");
                break;
            case R.id.image_menu:
                System.out.println("image_menu");
                break;
            case R.id.tv_paste:
                System.out.println("menu2_tv_paste");
                //ContentFragment fragment = getCurrentShowFragment();
                //getCurrentShowFragment().operaItem(true, 1);
                if ("复制到此".equals(((TextView)v).getText().toString())) {
                    getCurrentShowFragment().copy(mCheckItems);
                } else {
                    getCurrentShowFragment().cut(mCheckItems);
                }
                setShowWhichFoolMenuBar(0);
                break;
            case R.id.tv_create:
                System.out.println("menu2_tv_create");
                break;
            case R.id.tv_cancle:
                System.out.println("menu2_tv_cancle");
                isFloorMenu2Mode = false;
                getCurrentShowFragment().cancelCheckedItem();
                getCurrentShowFragment().operaItem(false, 0);
                setShowWhichFoolMenuBar(0);
                break;
            case R.id.tv_property2:
                System.out.println("menu2_tv_property2");
                break;
            case R.id.menu_exit:
                finish();
                break;
        }
    }

    private void setShowWhichFoolMenuBar(int which) {
        if (which == 1) {
            mLinearLayoutFloorMenuBar.setVisibility(View.GONE);
            mLinearLayoutFileHandleMenu2.setVisibility(View.GONE);
            mLinearLayoutFileHandleMenu1.setVisibility(View.VISIBLE);
        } else if (which == 2) {
            mLinearLayoutFloorMenuBar.setVisibility(View.GONE);
            mLinearLayoutFileHandleMenu1.setVisibility(View.GONE);
            mLinearLayoutFileHandleMenu2.setVisibility(View.VISIBLE);
        } else {
            mLinearLayoutFileHandleMenu1.setVisibility(View.GONE);
            mLinearLayoutFileHandleMenu2.setVisibility(View.GONE);
            mLinearLayoutFloorMenuBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ContentFragment fragment = getCurrentShowFragment();
        if (fragment != null) {
            if (isChecked) {
                fragment.checkAllItem();
            } else {
                fragment.cancelCheckedItem();
                setShowWhichFoolMenuBar(0);
            }
        }
    }

    public void onCheckBoxClick(boolean isChecked) {
        if (!isFloorMenu2Mode) {
            if (isChecked) {
                setShowWhichFoolMenuBar(1);
            } else {
                setShowWhichFoolMenuBar(0);
                checkBoxIsCheck();
            }
        }
    }

    private int[] getIntArray() {
        if (mClickLocation != null) {
            return mClickLocation;
        }
        return mClickLocation = new int[2];
    }

    private PopupWindow initPopupWindow(int layoutId, int width) {
        View view = getLayoutInflater().inflate(layoutId, null);
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        Toast.makeText(this,view.getMeasuredWidth()+"<-----w h------>"+view.getMeasuredHeight(),Toast.LENGTH_LONG ).show();

        PopupWindow popupWindow = new PopupWindow(view, width, view.getMeasuredHeight(), true);
        popupWindow.setBackgroundDrawable(new ColorDrawable());
        popupWindow.setOutsideTouchable(false);
        return popupWindow;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
