package com.lu.filemanager2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.chrisbanes.photoview.PhotoView;
import com.lu.App;
import com.lu.activity.AudioActivity;
import com.lu.activity.VideoActivity;
import com.lu.adapter.FragmentAdapter;
import com.lu.activity.BasedActivity;
import com.lu.filemanager2.databinding.ActivityMainBinding;
import com.lu.fragment.ContentFragment;
import com.lu.model.TempItem;
import com.lu.model.FileItem;
import com.lu.utils.FileUtils;
import com.lu.utils.PermissionUtils;
import com.lu.utils.SharePreferenceUtils;
import com.lu.utils.ShellUtils;
import com.lu.utils.TimeUtils;
import com.lu.utils.ToastUitls;
import com.lu.view.MyViewPager;
import com.lu.view.DialogManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends BasedActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private ActivityMainBinding mViewBind;
    private PhotoView mPhotoView;

    private TabLayout mTabLayout;
    private MyViewPager mViewPager;

    private int offes;
    private PopupWindow mPopupWindowFloorBarMenu;
    private PopupWindow mPopupWindowFloorBarAdd;
    private PopupWindow mPopupWindowFloorBarSort;
    private PopupWindow mPopupWindowFloorBar1Menu;
    private AlertDialog mPopupWindowFloorBarSearch;

    private AlertDialog mPropertyDialog;
    private HandleFile mHandleFile;

    private int[] mClickLocation;
    private static final int menu = 1;
    private static final int add = 2;
    public static int isWhat;
    private static final int DELETE = 101;
    private static final int CLOSE_TAG = 102;
    private static final int PROPERTY = 103;
    public static final int LOOK_EDIT = 104;
    public static final int MEDIA_VIDEO = 105;
    public static final int MEDIA_AUDIO = 106;

    private boolean isFloorMenu2Mode;

    private Set<FileItem> mCheckItems;
    private Set<FileItem> mCheckItems2;
    private Set<Integer> mPidSet;

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
        mHandleFile = new HandleFile();
        FileUtils.get().setOnHandleFileListener(mHandleFile);
        offes = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, getResources().getDisplayMetrics());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println("pid---------->"+getIntent().getIntExtra("pid", 0));
        System.out.println("pid2---------->"+intent.getIntExtra("pid", 0));
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharePreferenceUtils.saveFragmentVisibleIndex(getCurrentShowFragment().getIndex());
    }

    private void initLayout() {
        mPhotoView = mViewBind.mainPhotoView;
        mPhotoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
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
                    int addWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f, getResources().getDisplayMetrics());
                    mPopupWindowFloorBarAdd = initPopupWindow(R.layout.floor_menu_add, addWidth, 0);
                    LinearLayout layout = (LinearLayout) mPopupWindowFloorBarAdd.getContentView();
                    for (int i = 1; i < layout.getChildCount(); i++) {
                        if (layout.getChildAt(i) instanceof TextView) {
                            layout.getChildAt(i).setOnClickListener(this);
                        }
                    }
                }
                mPopupWindowFloorBarAdd.showAtLocation(v, Gravity.NO_GRAVITY, location[0] + offes, location[1] - mPopupWindowFloorBarAdd.getHeight() - offes);
                break;
            case R.id.floor_menu_search:
                if (mPopupWindowFloorBarSearch == null) {
                    AlertDialog.Builder searchBuilder = new AlertDialog.Builder(this);
                    View searchView = getLayoutInflater().inflate(R.layout.floor_menu_search, null);
                    searchView.findViewById(R.id.floor_menu_search_cancel).setOnClickListener(this);
                    searchView.findViewById(R.id.floor_menu_search_confirm).setOnClickListener(this);
                    searchBuilder.setView(searchView);
                    mPopupWindowFloorBarSearch = searchBuilder.create();
                    mPopupWindowFloorBarSearch.show();
                    WindowManager.LayoutParams params = mPopupWindowFloorBarSearch.getWindow().getAttributes();
                    params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, getResources().getDisplayMetrics());
                    mPopupWindowFloorBarSearch.getWindow().setAttributes(params);
                    return;
                }
                mPopupWindowFloorBarSearch.show();
                //mPopupWindowFloorBarSearch.showAtLocation(v, Gravity.CENTER, 0, - 60);
                break;
            case R.id.floor_menu_sort:
                if (mPopupWindowFloorBarSort == null) {
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int sortWidth = dm.widthPixels < 300 ? -2 : (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, dm);
                    mPopupWindowFloorBarSort = initPopupWindow(R.layout.floor_menu_sort, sortWidth, -2);
                    RadioGroup radioGroup = (RadioGroup) ((ScrollView) mPopupWindowFloorBarSort.getContentView()).getChildAt(0);
                    radioGroup.check(SharePreferenceUtils.getFileSortButtonId());
                    radioGroup.setOnCheckedChangeListener(this);
                }

                mPopupWindowFloorBarSort.showAtLocation(v, Gravity.CENTER, 0, 0);
                break;
            case R.id.floor_menu_close:
                //close current tab
                if (mFragmentList.size() > 1) {
                    isWhat = CLOSE_TAG;
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this, 2);
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
                    int menuWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180f, getResources().getDisplayMetrics());
                    mPopupWindowFloorBarMenu = initPopupWindow(R.layout.app_menu, menuWidth, 0);
                    LinearLayout menuLayout = (LinearLayout) mPopupWindowFloorBarMenu.getContentView();
                    for (int i = 1; i < menuLayout.getChildCount(); i++) {
                        if (menuLayout.getChildAt(i) instanceof TextView) {
                            menuLayout.getChildAt(i).setOnClickListener(this);
                        }
                    }
                }
                mPopupWindowFloorBarMenu.showAtLocation(v, Gravity.NO_GRAVITY, location2[0] + v.getWidth() - mPopupWindowFloorBarMenu.getWidth() - offes, location2[1] - mPopupWindowFloorBarMenu.getHeight() - offes);
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
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this, 2);
                    ((TextView)closeObjD[1]).setText(getString(R.string.rename));
                    ((TextView)closeObjD[2]).setText(getString(R.string.property_content));
                    ((AlertDialog)closeObjD[0]).show();
                } else {
                    rename(null);
                }
                break;
            case R.id.msg_confirm_execute:
                //执行脚本文件 或者 播放媒体
                ((AlertDialog)DialogManager.get().getMsgConfirmDialog(this, this, 0)[0]).dismiss();
                if (isWhat == MEDIA_VIDEO) {
                    Intent activityIntent = new Intent(this, VideoActivity.class);
                    activityIntent.putExtra("path", v.getTag().toString());
                    startActivity(activityIntent);
                }
                if (isWhat == MEDIA_AUDIO) {
                    Intent activityIntent = new Intent(this, AudioActivity.class);
                    activityIntent.putExtra("path", v.getTag().toString());
                    startActivity(activityIntent);
                }

                break;
            case R.id.msg_confirm_cancel:
                ((AlertDialog)DialogManager.get().getMsgConfirmDialog(this, this, 0)[0]).dismiss();
                break;
            case R.id.msg_confirm_confirm:
                ((AlertDialog)DialogManager.get().getMsgConfirmDialog(this, this, 0)[0]).dismiss();
                System.out.println("iswhat=" + isWhat);
                if (isWhat == DELETE) {
                    doDelete();
                }
                if (isWhat == CLOSE_TAG) {
                    int index = getCurrentShowFragment().getIndex();
                    System.out.println("index="+ index);
                    removeTab(index);
                }
                if (isWhat == LOOK_EDIT) {
                    FileUtils.get().do_text(getCurrentShowFragment().getIndex(), v.getTag().toString(), App.tempFilePath, 'l');
                }
                if (isWhat == MEDIA_VIDEO) {
                    //media open way
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + v.getTag().toString()), "video/*");
                    startActivity(intent);
                }
                if (isWhat == MEDIA_AUDIO) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + v.getTag().toString()), "audio/*");
                    startActivity(intent);
                }
                break;
            case R.id.image_property:
                System.out.println("menu1_image_property");
                if (getCurrentShowFragment().getCheckedItem().size() > 1) {
                    isWhat = PROPERTY;
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this, 2);
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
                System.out.println("handle1_image_menu");
                LinearLayout layout = null;
                if (mPopupWindowFloorBar1Menu == null) {
                    int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180f, getResources().getDisplayMetrics());
                    mPopupWindowFloorBar1Menu = initPopupWindow(R.layout.floor_menu1_menu, width, 0);
                    layout = (LinearLayout) mPopupWindowFloorBar1Menu.getContentView();
                    for (int i = 1; i < layout.getChildCount(); i++) {
                        if (layout.getChildAt(i) instanceof TextView) {
                            layout.getChildAt(i).setOnClickListener(this);
                        }
                    }
                }
                layout = (LinearLayout) mPopupWindowFloorBar1Menu.getContentView();
                Set<FileItem> set = getCurrentShowFragment().getCheckedItem();
                if (layout.getChildAt(4).getVisibility() == View.GONE) {
                    layout.getChildAt(4).setVisibility(View.VISIBLE);
                }
                if (set.size() == 1 && !set.iterator().next().isFolder()) {
                    if (layout.getChildAt(0).getVisibility() == View.GONE) {
                        layout.getChildAt(0).setVisibility(View.VISIBLE);
                    }
                } else {
                    layout.getChildAt(0).setVisibility(View.GONE);
                }
                for (FileItem item : set) {
                    if (item.isFolder()) {
                        layout.getChildAt(4).setVisibility(View.GONE);
                        break;
                    }
                }
                int menu1Location[] = getIntArray();
                v.getLocationOnScreen(menu1Location);
                mPopupWindowFloorBar1Menu.showAtLocation(v, Gravity.NO_GRAVITY, menu1Location[0] + v.getWidth() - mPopupWindowFloorBar1Menu.getWidth() - offes, menu1Location[1] - mPopupWindowFloorBar1Menu.getHeight() - offes);
                break;
            case R.id.menu1_open_way:

                break;
            case R.id.menu1_permissionset:
                mPopupWindowFloorBar1Menu.dismiss();
                getCurrentShowFragment().preparePermissionSetDialog(true);
                break;
            case R.id.menu1_sendto:
                break;
            case R.id.menu1_text_editorlook:
                break;
            case R.id.menu1_compress:
                break;
            case R.id.menu1_create_link:
                break;
            case R.id.tv_paste:
                System.out.println("menu2_tv_paste");
                //ContentFragment fragment = getCurrentShowFragment();
                //getCurrentShowFragment().operaItem(true, 1);
                if ("复制到此".equals(((TextView)v).getText().toString())) {
                    mHandleFile.mWhich = 1;
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
                    mHandleFile.mWhich = 2;
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
                    mPopupWindowFloorBarAdd = initPopupWindow(R.layout.floor_menu_add, (int) (metrics.widthPixels / ((float) 2.3)), 0);
                    LinearLayout ll = (LinearLayout) mPopupWindowFloorBarAdd.getContentView();
                    for (int i = 1; i < ll.getChildCount(); i++) {
                        if (ll.getChildAt(i) instanceof TextView) {
                            ll.getChildAt(i).setOnClickListener(this);
                        }
                    }
                }
                mPopupWindowFloorBarAdd.showAtLocation(v, Gravity.NO_GRAVITY, location2Create[0] + v.getWidth()/2  - 1, location2Create[1] - mPopupWindowFloorBarAdd.getHeight() - offes);
                break;
            case R.id.tv_cancle:
                System.out.println("menu2_tv_cancle");
                isFloorMenu2Mode = false;
                getCurrentShowFragment().cancelCheckedItem();
                setShowWhichFoolMenuBar(0);
                break;
            case R.id.tv_property2:
                System.out.println("menu2_tv_property2");
                if (mCheckItems.size() > 1) {
                    isWhat = PROPERTY;
                    Object closeObjD[] = DialogManager.get().getMsgConfirmDialog(this,this, 2);
                    ((TextView)closeObjD[1]).setText(getString(R.string.property));
                    ((TextView)closeObjD[2]).setText(getString(R.string.property_content));
                    ((AlertDialog)closeObjD[0]).show();
                } else {
                    showProperty(null);
                }
                break;
            case R.id.menu_exit:
                saveFragmentState();
                mPopupWindowFloorBarMenu.dismiss();
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
                ///System.out.println("tip_confirm--->" + PermissionUtils.arrays[1] + " " + mMounts[1]);
                //ToastUitls.showLMsgAtCenter(mMounts[0] + " " + mMounts[1]);
                if (ShellUtils.get().isRoot()) {
                    FileUtils.get().mountRW(PermissionUtils.arrays[1],PermissionUtils.arrays[2],PermissionUtils.arrays[3], getCurrentShowFragment().getIndex());
                } else {
                    getCurrentShowFragment().requestRoot(ContentFragment.REQ_ROOT_MOUNT_RW);
                }
                DialogManager.get().createTiPDialog(this, this).dismiss();
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
                    int position = 0;
                    if (isFloorMenu2Mode) {
                        old = mCheckItems2.iterator().next().getPath();
                        position = mCheckItems2.iterator().next().position();
                    } else {
                        old = mCheckItems.iterator().next().getPath();
                        position = mCheckItems.iterator().next().position();
                    }
                    getCurrentShowFragment().cancelCheckedItem();
                    getCurrentShowFragment().rename(old, strName, position);
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
            case R.id.progress_dialog_backrun:
                mHandleFile.mWhich = 0;
                ((AlertDialog)DialogManager.get().getProgressConfirmDialog(this, this)[0]).dismiss();
                break;
            case R.id.progress_dialog_cancel:
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
            tv[3].setText(getString(R.string.size_colon) + FileUtils.getFormatByte(selItem.size()));
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
            if (isFloorMenu2Mode) {
                mCheckItems2().add(item);
            } else {
                mCheckItems().add(item);
            }
        }
        isWhat = DELETE;
        System.out.println("delete---->" + item.getPath());
        Object msgConfirmDialog[] = DialogManager.get().getMsgConfirmDialog(this, this, 2);
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
        String obj2[] = PermissionUtils.isOnlyReadFileSys(checkItems.iterator().next().getPath());
        if (Boolean.parseBoolean(obj2[0])) {
            if (mMounts == null) {
                mMounts = new String[2];
            }
            mMounts[0] = obj2[1];
            mMounts[1] = obj2[2];
            //System.out.println(obj2[1] + " " + itemSet.iterator().next().getPath() + "------->is only read file sys");
            DialogManager.get().createTiPDialog(this, this).show();
            return;
        }
        getCurrentShowFragment().cancelCheckedItem();
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
        getCurrentShowFragment().cancelCheckedItem();
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
        getCurrentShowFragment().cancelCheckedItem();
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

    private class HandleFile implements FileUtils.onHandleFileListener {
        //获取NotificationManager实例
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        private Object[] progressObj;
        public int mWhich;
        public int mPid;
        public HandleFile() {
            mPidSet = new HashSet<>();
            progressObj = DialogManager.get().getProgressConfirmDialog(MainActivity.this, MainActivity.this);
        }
        @Override
        public void onCpAction(TempItem item) {
            if (item.isOver) {
                ((AlertDialog)progressObj[0]).dismiss();
                notifyManager.cancel(item.pid);
                ToastUitls.showSMsg("文件复制完成");
            } else {
                //System.out.println("totalSize:" + item.totalSize + "  curenSize:" + item.currentSize);
                if (!mPidSet.contains(item.pid)) {
                    mPidSet.add(item.pid);
                    mPid = item.pid;
                    sendNotification(item.pid, "正在复制到:" + item.path);
                }
                if (mWhich == 0) {
                    if (((AlertDialog)progressObj[0]).isShowing()) {
                        ((AlertDialog)progressObj[0]).dismiss();
                    }
                }
                if (mWhich == 1 && mPid == item.pid) {
                    showProgressDialog(item, R.string.copy_file, R.string.copy_ed, R.string.copy_ing);
                }
            }
        }

        @Override
        public void onMvAction(TempItem item) {
            if (item.isOver) {
                ((AlertDialog)progressObj[0]).dismiss();
                notifyManager.cancel(item.pid);
                ToastUitls.showSMsg("文件移动完成");
            } else {
                if (!mPidSet.contains(item.pid)) {
                    mPidSet.add(item.pid);
                    mPid = item.pid;
                    sendNotification(item.pid, "正在移动到:" + item.path);
                }
                if (mWhich == 0) {
                    if (((AlertDialog)progressObj[0]).isShowing()) {
                        ((AlertDialog)progressObj[0]).dismiss();
                    }
                }
                if (mWhich == 2 && mPid == item.pid) {
                    showProgressDialog(item, R.string.move_file, R.string.move_ed, R.string.move_ing);
                }
            }
        }

        private void showProgressDialog(TempItem item, int titleId, int contId, int contId2) {
            if (!((AlertDialog)progressObj[0]).isShowing()) {
                ((AlertDialog)progressObj[0]).show();
            }
            ((TextView)progressObj[1]).setText(titleId);
            ((TextView)progressObj[2]).setText(getString(R.string.total_size) + "：" + FileUtils.getFormatByte(item.totalSize));
            ((TextView)progressObj[3]).setText(getString(contId) + "：" + FileUtils.getFormatByte(item.currentSize));
            ((TextView)progressObj[4]).setText(getString(contId2) + "：" + item.path);
            if (item.totalSize > Integer.MAX_VALUE) {
                if (((ProgressBar)progressObj[5]).getMax() != 100000000) {
                    ((ProgressBar)progressObj[5]).setMax(100000000);
                }
                ((ProgressBar)progressObj[5]).setProgress((int) (((float) item.currentSize / item.totalSize) * 100000000));
            } else {
                if (((ProgressBar)progressObj[5]).getMax() != item.totalSize) {
                    ((ProgressBar)progressObj[5]).setMax((int) item.totalSize);
                }
                ((ProgressBar)progressObj[5]).setProgress((int) item.currentSize);
            }
        }

        @Override
        public void onDelAction(TempItem item) {
            if (item.isOver) {
                ToastUitls.showSMsg("完成");
            }
        }

        private void sendNotification(int pid, String title) {
            //实例化NotificationCompat.Builde并设置相关属性
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.putExtra("pid", pid);
            PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this)
                    //设置小图标
                    .setSmallIcon(R.mipmap.ic_launcher)
                    //设置通知标题
                    .setContentTitle(title)
                    //设置通知内容
                    .setContentText("请稍等...")
                    .setProgress(100, 0, true)
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent);
            //通过builder.build()方法生成Notification对象,并发送通知,id=1
            notifyManager.notify(pid, builder.build());
        }
    };

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

    private PopupWindow initPopupWindow(int layoutId, int width, int height) {
        View view = getLayoutInflater().inflate(layoutId, null);
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        height = height == 0 ? view.getMeasuredHeight() : height;
        PopupWindow popupWindow = new PopupWindow(view, width, height, true);
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
                getCurrentShowFragment().sort(FileUtils.SORT_BY_FILE_TYPE);
                SharePreferenceUtils.saveFileSort(0);
                break;
            case R.id.sort_date_asc:
                getCurrentShowFragment().sort(FileUtils.SORT_BY_FILE_DATE_ASC);
                SharePreferenceUtils.saveFileSort(1);
                break;
            case R.id.sort_date_desc:
                getCurrentShowFragment().sort(FileUtils.SORT_BY_FILE_DATE_DESC);
                SharePreferenceUtils.saveFileSort(2);
                break;
            case R.id.sort_name_asc:
                getCurrentShowFragment().sort(FileUtils.SORT_BY_FILE_NAME_ASC);
                SharePreferenceUtils.saveFileSort(3);
                break;
            case R.id.sort_name_desc:
                getCurrentShowFragment().sort(FileUtils.SORT_BY_FILE_NAME_DESC);
                SharePreferenceUtils.saveFileSort(4);
                break;
            case R.id.sort_size_asc:
                getCurrentShowFragment().sort(FileUtils.SORT_BY_FILE_SIZE_ASC);
                SharePreferenceUtils.saveFileSort(5);
                break;
            case R.id.sort_size_desc:
                getCurrentShowFragment().sort(FileUtils.SORT_BY_FILE_SIZE_DESC);
                SharePreferenceUtils.saveFileSort(6);
                break;
        }
    }
}
