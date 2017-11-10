package com.lu.filemanager2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.lu.App;
import com.lu.adapter.FragmentAdapter;
import com.lu.activity.BasedActivity;
import com.lu.filemanager2.databinding.ActivityMainBinding;
import com.lu.fragment.ContentFragment;
import com.lu.model.FileItem;
import com.lu.utils.FileUtil;
import com.lu.utils.SharePreferenceUtils;
import com.lu.utils.TimeUtils;
import com.lu.view.MyViewPager;
import com.lu.view.ViewManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends BasedActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private ActivityMainBinding mViewBind;
    private PhotoView mPhotoView;

    private TabLayout mTabLayout;
    private MyViewPager mViewPager;

    private PopupWindow mPopupWindowFloorBarMenu;
    private PopupWindow mPopupWindowFloorBarAdd;
    private PopupWindow mPopupWindowFloorBarSort;
    private PopupWindow mPopupWindowFloorBarSearch;

    private AlertDialog mPropertyDialog;

    private int[] mClickLocation;
    private static final int menu = 1;
    private static final int add = 2;

    private boolean isFloorMenu2Mode;

    private Set<FileItem> mCheckItems;

    private List<Fragment> mFragmentList;
    private List<String> mFragmentTitleList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mViewBind = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initLayout();
        setListener();
        App.initMyLs();

        LinearLayout layout = (LinearLayout) mTabLayout.getChildAt(0);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        layout.setDividerDrawable(ContextCompat.getDrawable(this, R.drawable.tablayout_tab_divider_vertical));
        mTabLayout.setupWithViewPager(mViewPager);

        mFragmentList = new ArrayList<>();
        mFragmentTitleList = new ArrayList<>();
        int count = SharePreferenceUtils.getFragmentCount();
        String title = null;
        for (int i = 0; i < count; i++) {
            title = SharePreferenceUtils.getCurrentPathName(i);
            addNewTab(i, title, null);
        }
        mViewPager.setAdapter(new FragmentAdapter(mFragmentList, mFragmentTitleList, getSupportFragmentManager()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        int visibleIndex = SharePreferenceUtils.getVisibleIndex();
        mViewPager.setCurrentItem(visibleIndex);
    }

    private void initLayout() {
        mPhotoView = mViewBind.mainPhotoView;
        mTabLayout = mViewBind.tabLayout;
        mViewPager = mViewBind.viewPager;
    }

    private void setListener() {
        int childCount = mViewBind.floorMenuBar.linearlayoutFloorMenuBar.getChildCount();
        for (int i = 0; i < childCount; i++) {
            mViewBind.floorMenuBar.linearlayoutFloorMenuBar.getChildAt(i).setOnClickListener(this);
        }

        childCount = mViewBind.fileHandleMenu1.linearlayoutFilehandle1.getChildCount();
        for (int i = 0; i < childCount; i++) {
            mViewBind.fileHandleMenu1.linearlayoutFilehandle1.getChildAt(i).setOnClickListener(this);
        }

        childCount = mViewBind.fileHandleMenu2.linearlayoutFilehandle2.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (mViewBind.fileHandleMenu2.linearlayoutFilehandle2.getChildAt(i) instanceof TextView) {
                mViewBind.fileHandleMenu2.linearlayoutFilehandle2.getChildAt(i).setOnClickListener(this);
            }
        }
    }

    public void setTabTitle(int index, String name) {
        mFragmentTitleList.set(index, name);
        mViewPager.getAdapter().notifyDataSetChanged();
    }

    private void addNewTab(int index, String title, String rootDir) {
        ContentFragment fragment = new ContentFragment();
        fragment.setRootDir(rootDir);
        fragment.setIndex(index);
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }

    private void removeTab(int index) {
        Fragment fragment = mFragmentList.get(index);
        mFragmentList.remove(index);
        mFragmentTitleList.remove(index);
        for (int i = index; i < mFragmentList.size(); i++) {
            ((ContentFragment)mFragmentList.get(i)).setIndex(i);
        }
        System.out.println("removeTab-->");
        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        mViewPager.getAdapter().notifyDataSetChanged();
        SharePreferenceUtils.clearFragmentState(mFragmentList.size());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                //处理菜单键
                onClick(mViewBind.floorMenuBar.floorMenuMenu);
                return true;
            case KeyEvent.KEYCODE_BACK:
                //处理返回键
                //找到当前显示的fragment
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
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void saveFragmentState() {
        SharePreferenceUtils.saveFragmentCount(mFragmentList.size());
        for (Fragment fragment : mFragmentList) {
            ((ContentFragment)fragment).saveCurrentState();
        }
    }

    @SuppressLint("RestrictedApi")
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
                    LinearLayout layout = (LinearLayout) mPopupWindowFloorBarAdd.getContentView();
                    for (int i = 1; i < layout.getChildCount(); i++) {
                        if (layout.getChildAt(i) instanceof TextView) {
                            layout.getChildAt(i).setOnClickListener(this);
                        }
                    }
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
                    RadioGroup radioGroup = (RadioGroup) mPopupWindowFloorBarSort.getContentView();
                    radioGroup.check(SharePreferenceUtils.getFileSortButtonId());
                    radioGroup.setOnCheckedChangeListener(this);
                }
                mPopupWindowFloorBarSort.showAtLocation(v, Gravity.CENTER, 0, 0);
                break;
            case R.id.floor_menu_close:
                //close current tab
                int index = getCurrentShowFragment().getIndex();
                System.out.println("index="+ index);
                if (index >= 0 && mFragmentList.size() > 1) {
                    removeTab(index);
                }
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
                    LinearLayout menuLayout = (LinearLayout) mPopupWindowFloorBarMenu.getContentView();
                    for (int i = 1; i < menuLayout.getChildCount(); i++) {
                        if (menuLayout.getChildAt(i) instanceof TextView) {
                            menuLayout.getChildAt(i).setOnClickListener(this);
                        }
                    }
                }
                mPopupWindowFloorBarMenu.showAtLocation(v, Gravity.NO_GRAVITY, location2[0] + v.getWidth() - mPopupWindowFloorBarMenu.getWidth() - 1, location2[1] - mPopupWindowFloorBarMenu.getHeight() - 5);
                break;
            case R.id.image_copy:
                System.out.println("menu1_image_copy");
                mCheckItems = getCurrentShowFragment().getCheckedItem();
                isFloorMenu2Mode = true;
                mViewBind.fileHandleMenu2.tvPaste.setText("复制到此");
                setShowWhichFoolMenuBar(2);
                getCurrentShowFragment().operaItem(true, 0);
                break;
            case R.id.image_cut:
                System.out.println("menu1_image_cut");
                mCheckItems = getCurrentShowFragment().getCheckedItem();
                isFloorMenu2Mode = true;
                mViewBind.fileHandleMenu2.tvPaste.setText("移动到此");
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
                TextView tv[] = ViewManager.getInstance().getPropertyTvArray();
                if (mPropertyDialog == null) {
                    mPropertyDialog = ViewManager.getInstance().createPropertyDialog(this, this);
                }
                FileItem selItem = getCurrentShowFragment().getCheckedItem().iterator().next();
                tv[0].setText(getString(R.string.name_colon) + selItem.getName());
                tv[1].setText(getString(R.string.path_colon) + selItem.getPath());
                tv[2].setText(getString(R.string.perm_colon) + selItem.getPer());
                tv[4].setText(getString(R.string.time_colon) + TimeUtils.getFormatDateTime(selItem.lastModified()));
                tv[5].setText(getString(R.string.owner_colon) + selItem.getUser());
                tv[6].setText(getString(R.string.usergroup_colon) + selItem.getGroup());
                if (selItem.isLink()) {
                    tv[7].setVisibility(View.VISIBLE);
                    tv[7].setText(getString(R.string.linkto_colon) + selItem.linkTo());
                } else {
                    tv[7].setVisibility(View.GONE);
                }
                if (selItem.isFolder()) {
                    tv[3].setText(getString(R.string.size_colon).toString() + getString(R.string.counting));
                    getCurrentShowFragment().countDirSize(selItem.getPath());
                } else {
                    tv[3].setText(getString(R.string.size_colon) + FileUtil.getFormatByte(selItem.size()));
                }
                mPropertyDialog.show();
                break;
            case R.id.property_confirm:
                mPropertyDialog.dismiss();
                break;
            case R.id.image_checkbox:
                //check all or cancel all checked
                getCurrentShowFragment().onCheckedChanged();
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
                saveFragmentState();
                finish();
                break;
            case R.id.floor_menu_add_folder:
                break;
            case R.id.floor_menu_add_file:
                break;
            case R.id.floor_menu_add_roottab:
                // root tab
                mPopupWindowFloorBarAdd.dismiss();
                int pos = mFragmentList.size();
                addNewTab(pos, getString(R.string.root_dir), "/");
                mViewPager.getAdapter().notifyDataSetChanged();
                mViewPager.setCurrentItem(pos);
                break;
            case R.id.floor_menu_add_phonetab:
                // phone tab
                mPopupWindowFloorBarAdd.dismiss();
                int pos2 = mFragmentList.size();
                addNewTab(pos2, getString(R.string.phone_storage), Environment.getExternalStorageDirectory().getAbsolutePath());
                mViewPager.getAdapter().notifyDataSetChanged();
                mViewPager.setCurrentItem(pos2);
                break;
        }
    }

    public boolean isLookImage() {
        return mPhotoView.getVisibility() == View.VISIBLE;
    }

    public void prepareLookImage(String imgPath) {
        mTabLayout.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
        mViewBind.floorMenuBar.linearlayoutFloorMenuBar.setVisibility(View.GONE);
        mPhotoView.setImageURI(Uri.parse(imgPath));
        mPhotoView.setVisibility(View.VISIBLE);
    }

    public void stopLookImage() {
        mPhotoView.setVisibility(View.GONE);
        mTabLayout.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
        mViewBind.floorMenuBar.linearlayoutFloorMenuBar.setVisibility(View.VISIBLE);
    }

    private void setShowWhichFoolMenuBar(int which) {
        if (which == 1) {
            mViewBind.floorMenuBar.linearlayoutFloorMenuBar.setVisibility(View.GONE);
            mViewBind.fileHandleMenu2.linearlayoutFilehandle2.setVisibility(View.GONE);
            mViewBind.fileHandleMenu1.linearlayoutFilehandle1.setVisibility(View.VISIBLE);
        } else if (which == 2) {
            mViewBind.floorMenuBar.linearlayoutFloorMenuBar.setVisibility(View.GONE);
            mViewBind.fileHandleMenu1.linearlayoutFilehandle1.setVisibility(View.GONE);
            mViewBind.fileHandleMenu2.linearlayoutFilehandle2.setVisibility(View.VISIBLE);
        } else {
            mViewBind.fileHandleMenu1.linearlayoutFilehandle1.setVisibility(View.GONE);
            mViewBind.fileHandleMenu2.linearlayoutFilehandle2.setVisibility(View.GONE);
            mViewBind.floorMenuBar.linearlayoutFloorMenuBar.setVisibility(View.VISIBLE);
        }
    }

    public void onCheckBoxClick(boolean isChecked, int count) {
        mViewBind.fileHandleMenu1.imageProperty.setEnabled(count > 1 ? false : true);
        if (!isFloorMenu2Mode) {
            if (isChecked) {
                setShowWhichFoolMenuBar(1);
            } else {
                setShowWhichFoolMenuBar(0);
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

        PopupWindow popupWindow = new PopupWindow(view, width, view.getMeasuredHeight(), true);
        popupWindow.setBackgroundDrawable(new ColorDrawable());
        popupWindow.setOutsideTouchable(false);
        return popupWindow;
    }

    @Override
    protected void onDestroy() {
        System.out.println("activity onDestroy");
        saveFragmentState();
        super.onDestroy();
    }

    //排序的radioGroup button
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sort_type:
                getCurrentShowFragment().sort(FileUtil.SORT_BY_FILE_TYPE);
                SharePreferenceUtils.saveFileSort(0);
                break;
            case R.id.sort_date_asc:
                getCurrentShowFragment().sort(FileUtil.SORT_BY_FILE_DATE_ASC);
                SharePreferenceUtils.saveFileSort(1);
                break;
            case R.id.sort_date_desc:
                getCurrentShowFragment().sort(FileUtil.SORT_BY_FILE_DATE_DESC);
                SharePreferenceUtils.saveFileSort(2);
                break;
            case R.id.sort_name_asc:
                getCurrentShowFragment().sort(FileUtil.SORT_BY_FILE_NAME_ASC);
                SharePreferenceUtils.saveFileSort(3);
                break;
            case R.id.sort_name_desc:
                getCurrentShowFragment().sort(FileUtil.SORT_BY_FILE_NAME_DESC);
                SharePreferenceUtils.saveFileSort(4);
                break;
            case R.id.sort_size_asc:
                getCurrentShowFragment().sort(FileUtil.SORT_BY_FILE_SIZE_ASC);
                SharePreferenceUtils.saveFileSort(5);
                break;
            case R.id.sort_size_desc:
                getCurrentShowFragment().sort(FileUtil.SORT_BY_FILE_SIZE_DESC);
                SharePreferenceUtils.saveFileSort(6);
                break;
        }
    }
}
