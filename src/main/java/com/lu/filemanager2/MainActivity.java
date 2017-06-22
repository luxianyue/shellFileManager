package com.lu.filemanager2;

import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.lu.adapter.FragmentAdapter;
import com.lu.fragment.ContentFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private LinearLayout linearLayoutFloorMenuBar;
    private LinearLayout linearLayoutFileHandleMenu1;
    private LinearLayout linearLayoutFileHandleMenu2;

    private PopupWindow popupWindowFloorBarMenu;
    private PopupWindow popupWindowFloorBarAdd;
    private PopupWindow popupWindowFloorBarSort;
    private PopupWindow popupWindowFloorBarSearch;

    private int[] clickLocation;
    private static final int menu = 1;
    private static final int add = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
        setListener();
        tabLayout.setupWithViewPager(viewPager);
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
        viewPager.setAdapter(new FragmentAdapter(list, list2, getSupportFragmentManager()));
    }

    private void initLayout() {
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        linearLayoutFloorMenuBar = (LinearLayout) findViewById(R.id.linearlayout_floor_menu_bar);
        linearLayoutFileHandleMenu1 = (LinearLayout) findViewById(R.id.linearlayout_filehandle1);
        linearLayoutFileHandleMenu2 = (LinearLayout) findViewById(R.id.linearlayout_filehandle2);
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
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment != null && fragment instanceof ContentFragment) {
                        if (((ContentFragment) fragment).isShowToUser()) {
                            if (((ContentFragment) fragment).onKeyBack()) {
                                return true;
                            }
                            break;
                        }
                    }
                }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floor_menu_add:
                int location[] = getIntArray();
                v.getLocationOnScreen(location);
                if (popupWindowFloorBarAdd == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    popupWindowFloorBarAdd = initPopupWindow(R.layout.floor_menu_add, (int) (metrics.widthPixels / ((float) 2.3)));
                    popupWindowFloorBarAdd.getContentView().findViewById(R.id.floor_menu_add_file).setOnClickListener(this);
                    popupWindowFloorBarAdd.getContentView().findViewById(R.id.floor_menu_add_folder).setOnClickListener(this);
                }
                popupWindowFloorBarAdd.showAtLocation(v, Gravity.NO_GRAVITY, location[0] + v.getWidth()/2  - 1, location[1] - popupWindowFloorBarAdd.getHeight() - 10);
                break;
            case R.id.floor_menu_search:
                int locationSearch[] = getIntArray();
                v.getLocationOnScreen(locationSearch);
                if (popupWindowFloorBarSearch == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    popupWindowFloorBarSearch = initPopupWindow(R.layout.floor_menu_search, (int) (metrics.widthPixels*1.5 / ((float) 2)));
                }
                popupWindowFloorBarSearch.showAtLocation(v, Gravity.CENTER_HORIZONTAL, 0, v.getHeight()/2);
                break;
            case R.id.floor_menu_sort:
                int locationSort[] = getIntArray();
                v.getLocationOnScreen(locationSort);
                if (popupWindowFloorBarSort == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    popupWindowFloorBarSort = initPopupWindow(R.layout.floor_menu_sort, (int) (metrics.widthPixels*1.5 / ((float) 2)));
                }
                popupWindowFloorBarSort.showAtLocation(v, Gravity.CENTER, 0, 0);
                break;
            case R.id.floor_menu_close:
                break;
            case R.id.floor_menu_refresh:
                break;
            case R.id.floor_menu_menu:
                int location2[] = getIntArray();
                v.getLocationOnScreen(location2);
                if (popupWindowFloorBarMenu == null) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    popupWindowFloorBarMenu = initPopupWindow(R.layout.app_menu, (int) (metrics.widthPixels / ((float) 2.3)));
                    popupWindowFloorBarMenu.getContentView().findViewById(R.id.menu_settings).setOnClickListener(this);
                    popupWindowFloorBarMenu.getContentView().findViewById(R.id.menu_about).setOnClickListener(this);
                    popupWindowFloorBarMenu.getContentView().findViewById(R.id.menu_exit).setOnClickListener(this);
                }
                popupWindowFloorBarMenu.showAtLocation(v, Gravity.NO_GRAVITY, location2[0] + v.getWidth() - popupWindowFloorBarMenu.getWidth() - 1, location2[1] - popupWindowFloorBarMenu.getHeight() - 5);
                break;
            case R.id.image_copy:
                System.out.println("image_copy");
                break;
            case R.id.image_cut:
                System.out.println("image_cut");
                break;
            case R.id.image_delete:
                System.out.println("image_delete");
                break;
            case R.id.image_rename:
                System.out.println("image_rename");
                break;
            case R.id.image_property:
                System.out.println("image_property");
                break;
            case R.id.image_menu:
                System.out.println("image_menu");
                break;
            case R.id.tv_paste:
                System.out.println("tv_paste");
                break;
            case R.id.tv_create:
                System.out.println("tv_create");
                break;
            case R.id.tv_cancle:
                System.out.println("tv_cancle");
                break;
            case R.id.tv_property2:
                System.out.println("tv_property2");
                break;
            case R.id.menu_exit:
                finish();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        System.out.println(isChecked);
    }

    public void onCheckBoxClick(boolean isChecked) {
        if (isChecked) {
            linearLayoutFloorMenuBar.setVisibility(View.GONE);
            linearLayoutFileHandleMenu1.setVisibility(View.VISIBLE);
        } else {
            linearLayoutFileHandleMenu1.setVisibility(View.GONE);
            linearLayoutFloorMenuBar.setVisibility(View.VISIBLE);
        }
    }

    private int[] getIntArray() {
        if (clickLocation != null) {
            return clickLocation;
        }
        return clickLocation = new int[2];
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
