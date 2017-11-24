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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.chrisbanes.photoview.PhotoView;
import com.lu.App;
import com.lu.adapter.FragmentAdapter;
import com.lu.activity.BasedActivity;
import com.lu.filemanager2.databinding.ActivityMainBinding;
import com.lu.fragment.ContentFragment;
import com.lu.model.FileItem;
import com.lu.utils.FileUtil;
import com.lu.utils.PermissionUtils;
import com.lu.utils.SharePreferenceUtils;
import com.lu.utils.ShellUtil;
import com.lu.utils.TimeUtils;
import com.lu.utils.ToastUitls;
import com.lu.view.MyViewPager;
import com.lu.view.DialogManager;

import java.util.ArrayList;
import java.util.HashSet;
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
    private int isWhat;
    private static final int DELETE = 101;
    private static final int CLOSE_TAG = 102;
    private static final int PROPERTY = 103;

    private boolean isFloorMenu2Mode;

    private Set<FileItem> mCheckItems;
    private Set<FileItem> mCheckItems2;

    private List<Fragment> mFragmentList;
    private List<String> mFragmentTitleList;

    private String mMounts[];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBind = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initLayout();
        setListener();
        App.initTools();

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
        //boolean v = PermissionUtils.parseMounts("/system/bin/a");
        //System.out.println(PermissionUtils.getFs("system/bin/a") + " is " + v);
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
                if (isFloorMenu2Mode) {
                    isFloorMenu2Mode = false;
                    setShowWhichFoolMenuBar(0);
                    return true;
                }
                /*if (contentFragment.isItemOpera()) {
                    setShowWhichFoolMenuBar(0);
                    contentFragment.operaItem(false, 0);
                    contentFragment.cancelCheckedItem();
                    return true;
                }*/
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
                if (mPopupWindowFloorBarSearch == null) {
                    mPopupWindowFloorBarSearch = initPopupWindow(R.layout.floor_menu_search, 300);
                    mPopupWindowFloorBarSearch.getContentView().findViewById(R.id.floor_menu_search_cancel).setOnClickListener(this);
                    mPopupWindowFloorBarSearch.getContentView().findViewById(R.id.floor_menu_search_confirm).setOnClickListener(this);
                }
                mPopupWindowFloorBarSearch.showAtLocation(v, Gravity.CENTER, 0, - 60);
                break;
            case R.id.floor_menu_sort:
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
                if (mFragmentList.size() > 1) {
                    isWhat = CLOSE_TAG;
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this);
                    ((TextView)closeObjD[1]).setText(getString(R.string.msg_title_close));
                    ((TextView)closeObjD[2]).setText(getString(R.string.msg_content_close));
                    ((AlertDialog)closeObjD[0]).show();
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
                //mCheckItems = getCurrentShowFragment().getCheckedItem();
                copy(null);
                break;
            case R.id.image_cut:
                System.out.println("menu1_image_cut");
                move(null);
                //getCurrentShowFragment().cancelAllCheckedItem();
                break;
            case R.id.image_delete:
                System.out.println("menu1_image_delete");
                isWhat = DELETE;
                //mCheckItems = getCurrentShowFragment().getCheckedItem();
                delete(null);
                break;
            case R.id.image_rename:
                System.out.println("menu1_image_rename");
                //mCheckItems = getCurrentShowFragment().getCheckedItem();
                if (getCurrentShowFragment().getCheckedItem().size() > 1) {
                    isWhat = PROPERTY;
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this);
                    ((TextView)closeObjD[1]).setText(getString(R.string.rename));
                    ((TextView)closeObjD[2]).setText(getString(R.string.property_content));
                    ((AlertDialog)closeObjD[0]).show();
                } else {
                    rename(null);
                }
                break;
            case R.id.msg_confirm_cancel:
                ((AlertDialog)DialogManager.get().getMsgConfirmDialog(this, this)[0]).dismiss();
                break;
            case R.id.msg_confirm_confirm:
                ((AlertDialog)DialogManager.get().getMsgConfirmDialog(this, this)[0]).dismiss();
                System.out.println("iswhat=" + isWhat);
                if (isWhat == DELETE) {
                    doDelete();
                }
                if (isWhat == CLOSE_TAG) {
                    int index = getCurrentShowFragment().getIndex();
                    System.out.println("index="+ index);
                    removeTab(index);
                }
                isWhat = 0;
                break;
            case R.id.image_property:
                System.out.println("menu1_image_property");
                if (getCurrentShowFragment().getCheckedItem().size() > 1) {
                    isWhat = PROPERTY;
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this);
                    ((TextView)closeObjD[1]).setText(getString(R.string.property));
                    ((TextView)closeObjD[2]).setText(getString(R.string.property_content));
                    ((AlertDialog)closeObjD[0]).show();
                } else {
                    showProperty(getCurrentShowFragment().getCheckedItem().iterator().next());
                }
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
                    Object obj[] = PermissionUtils.isOnlyReadFileSys(mCheckItems.iterator().next().getPath());
                    if (Boolean.parseBoolean(obj[0].toString())) {
                        System.out.println(obj[1] + " " + mCheckItems.iterator().next().getPath() + "------->is only read file sys");
                        if (mMounts == null) {
                            mMounts = new String[2];
                        }
                        mMounts[0] = obj[1].toString();
                        mMounts[1] = obj[2].toString();
                        DialogManager.get().createTiPDialog(this, this).show();
                        break;
                    }
                    getCurrentShowFragment().cut(mCheckItems);
                }
                isFloorMenu2Mode = false;
                setShowWhichFoolMenuBar(0);
                break;
            case R.id.tv_create:
                System.out.println("menu2_tv_create");
                int location2Create[] = getIntArray();
                v.getLocationOnScreen(location2Create);
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
                mPopupWindowFloorBarAdd.showAtLocation(v, Gravity.NO_GRAVITY, location2Create[0] + v.getWidth()/2  - 1, location2Create[1] - mPopupWindowFloorBarAdd.getHeight() - 10);
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
                if (mCheckItems.size() > 1) {
                    isWhat = PROPERTY;
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this);
                    ((TextView)closeObjD[1]).setText(getString(R.string.property));
                    ((TextView)closeObjD[2]).setText(getString(R.string.property_content));
                    ((AlertDialog)closeObjD[0]).show();
                } else {
                    showProperty(null);
                }
                break;
            case R.id.menu_exit:
                saveFragmentState();
                finish();
                break;
            case R.id.floor_menu_add_folder:
                Object ndirDialog[] = DialogManager.get().createNewFileOrDirDialog(this, this);
                ((TextView)ndirDialog[1]).setText(getString(R.string.mk_dir));
                ((EditText)ndirDialog[2]).getText().clear();
                mPopupWindowFloorBarAdd.dismiss();
                ((AlertDialog)ndirDialog[0]).show();
                break;
            case R.id.floor_menu_add_file:
                Object fileDialog[] = DialogManager.get().createNewFileOrDirDialog(this, this);
                ((TextView)fileDialog[1]).setText(getString(R.string.mk_file));
                ((EditText)fileDialog[2]).getText().clear();
                mPopupWindowFloorBarAdd.dismiss();
                ((AlertDialog)fileDialog[0]).show();
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
            case R.id.tip_cancel:
                DialogManager.get().createTiPDialog(this, this).dismiss();
                break;
            case R.id.tip_confirm:
                System.out.println("tip_confirm--->" + mMounts[0] + " " + mMounts[1]);
                ToastUitls.showLMsgAtCenter(mMounts[0] + " " + mMounts[1]);
                break;
            case R.id.newfiledir_cancel:
                ((AlertDialog) DialogManager.get().createNewFileOrDirDialog(this, this)[0]).dismiss();
                break;
            case R.id.newfiledir_confirm:
                Object objDF[] = DialogManager.get().createNewFileOrDirDialog(this, this);
                AlertDialog dfDialog = (AlertDialog) objDF[0];
                String strName = ((EditText)objDF[2]).getText().toString().trim();
                if ("".equals(strName)) {
                    //editText.setInputType(InputType.TYPE_NULL);
                    dfDialog.dismiss();
                    break;
                }
                String ntv = ((TextView)objDF[1]).getText().toString();
                if (ntv.equals(getString(R.string.mk_dir))) {
                    getCurrentShowFragment().createDir(strName);
                    dfDialog.dismiss();
                }
                if (ntv.equals(getString(R.string.mk_file))) {
                    getCurrentShowFragment().createFile(strName);
                    dfDialog.dismiss();
                }
                if (ntv.equals(getString(R.string.rename))) {
                    String old = null;
                    if (isFloorMenu2Mode) {
                        old = mCheckItems2.iterator().next().getPath();
                    } else {
                        old = mCheckItems.iterator().next().getPath();
                    }
                    getCurrentShowFragment().cancelCheckedItem();
                    getCurrentShowFragment().rename(old, strName);
                    dfDialog.dismiss();
                }
                break;
            case R.id.msg_confirm:
                ((AlertDialog)DialogManager.get().getMsgDialog(this,this)[0]).dismiss();
                break;
            case R.id.floor_menu_search_cancel:
                mPopupWindowFloorBarSearch.dismiss();
                break;
            case R.id.floor_menu_search_confirm:
                break;
        }
    }

    public boolean isLookImage() {
        return mPhotoView.getVisibility() == View.VISIBLE;
    }

    public void showProperty(FileItem selItem) {
        TextView tv[] = DialogManager.get().getPropertyTvArray();
        if (mPropertyDialog == null) {
            mPropertyDialog = DialogManager.get().createPropertyDialog(this, this);
        }
        if (selItem == null) {
            selItem = mCheckItems.iterator().next();
        }

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
    }

    public void delete(FileItem item) {
        int size = 0;
        if (item == null) {
            if (isFloorMenu2Mode) {
                mCheckItems2().addAll(getCurrentShowFragment().getCheckedItem());
                item = mCheckItems2.iterator().next();
                size = mCheckItems2.size();
            } else {
                mCheckItems().addAll(getCurrentShowFragment().getCheckedItem());
                item = mCheckItems.iterator().next();
                size = mCheckItems.size();
            }
        } else {
            isWhat = DELETE;
            mCheckItems().add(item);
        }
        System.out.println("delete---->" + item.getPath());
        Object msgConfirmDialog[] = DialogManager.get().getMsgConfirmDialog(this, this);
        ((TextView)msgConfirmDialog[1]).setText(item.getName());
        if (item.isFolder()) {
            ((TextView)msgConfirmDialog[2]).setText(getString(R.string.msg_content_deldir));
        } else {
            ((TextView)msgConfirmDialog[2]).setText(getString(R.string.msg_content_delfile));
        }
        if (size > 1) {
            ((TextView)msgConfirmDialog[1]).setText(getString(R.string.msg_content_title_dels));
            ((TextView)msgConfirmDialog[2]).setText(getString(R.string.msg_content_dels));
        }
        ((AlertDialog)msgConfirmDialog[0]).show();
    }

    private void doDelete() {
        System.out.println("doDelete--");
        Set<FileItem> checkItems = null;
        if (isFloorMenu2Mode) {
            checkItems = mCheckItems2;
        } else {
            checkItems = mCheckItems;
        }
        Object obj2[] = PermissionUtils.isOnlyReadFileSys(checkItems.iterator().next().getPath());
        if (Boolean.parseBoolean(obj2[0].toString())) {
            if (mMounts == null) {
                mMounts = new String[2];
            }
            mMounts[0] = obj2[1].toString();
            mMounts[1] = obj2[2].toString();
            //System.out.println(obj2[1] + " " + itemSet.iterator().next().getPath() + "------->is only read file sys");
            DialogManager.get().createTiPDialog(this, this).show();
            return;
        }
        getCurrentShowFragment().del(checkItems);
    }

    public void rename(FileItem item) {
        if (item == null) {
            if (isFloorMenu2Mode) {
                mCheckItems2().addAll(getCurrentShowFragment().getCheckedItem());
                item = mCheckItems2.iterator().next();
            } else {
                mCheckItems().addAll(getCurrentShowFragment().getCheckedItem());
                item = mCheckItems.iterator().next();
            }
        } else {
            mCheckItems().add(item);
        }
        Object dirDialog[] = DialogManager.get().createNewFileOrDirDialog(this, this);
        ((TextView)dirDialog[1]).setText(getString(R.string.rename));
        ((EditText)dirDialog[2]).setText(item.getName());
        ((AlertDialog)dirDialog[0]).show();
    }

    public void copy(FileItem item) {
        if (item == null) {
            mCheckItems().addAll(getCurrentShowFragment().getCheckedItem());
        } else {
            mCheckItems().add(item);
        }
        isFloorMenu2Mode = true;
        mViewBind.fileHandleMenu2.tvPaste.setText("复制到此");
        setShowWhichFoolMenuBar(2);
        getCurrentShowFragment().operaItem(true, 0);
    }

    public void move(FileItem item) {
        if (item == null) {
            mCheckItems().addAll(getCurrentShowFragment().getCheckedItem());
        } else {
            mCheckItems().add(item);
        }
        isFloorMenu2Mode = true;
        mViewBind.fileHandleMenu2.tvPaste.setText("移动到此");
        setShowWhichFoolMenuBar(2);
        getCurrentShowFragment().operaItem(true, 0);
    }

    private Set<FileItem> mCheckItems() {
        if (mCheckItems != null) {
            mCheckItems.clear();
            return mCheckItems;
        }
        mCheckItems = new HashSet<>();
        return mCheckItems;
    }

    private Set<FileItem> mCheckItems2() {
        if (mCheckItems2 != null) {
            mCheckItems2.clear();
            return mCheckItems2;
        }
        mCheckItems2 = new HashSet<>();
        return mCheckItems2;
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
        //System.out.println(count + "----->onCheckBoxClick " + isChecked);
        //mViewBind.fileHandleMenu1.imageProperty.setEnabled(count > 1 ? false : true);
        //mViewBind.fileHandleMenu1.imageRename.setEnabled(count > 1 ? false : true);
        if (isChecked) {
            setShowWhichFoolMenuBar(1);
        } else {
            if (isFloorMenu2Mode) {
                setShowWhichFoolMenuBar(2);
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
        DialogManager.onDestroy();
        //ShellUtil.get().release();
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
