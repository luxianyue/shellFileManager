package com.lu.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.lu.adapter.FileListAdapter;
import com.lu.filemanager2.MainActivity;
import com.lu.filemanager2.R;
import com.lu.filemanager2.databinding.PermissionSetMenuBinding;
import com.lu.model.FileItem;
import com.lu.model.Path;
import com.lu.utils.FileUtil;
import com.lu.utils.SharePreferenceUtils;
import com.lu.utils.TimeUtils;
import com.lu.view.ViewManager;

import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Created by lu on 2016/10/23.
 */

public class ContentFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private ListView mListView;
    private FileListAdapter mFileListAdapter;

    private Stack<Path> mBackStack;

    private Path mCurrentPath;

    private TextView mTextViewPath;

    private FileUtil mFileUtil;

    private boolean isShowToUser;

    private boolean isFirstEnter;

    private TextView mPerSetFileName;

    private CheckBox mPerCheckBoxs[];

    private FileItem mLongSelFileItem;

    private AlertDialog mItemLongClickDialog;

    private AlertDialog mPermissionSetDialog;

    private AlertDialog mFilePropertyDialog;

    private TextView mPermissionOctalValueTextView;

    private int perFlagSet, owner, userGroup, other;

    private int mPermiss[];

    private int mIndex = -1;

    private String mInitDir;

    private String mExternalStoragePath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mBackStack == null) {
            mBackStack = new Stack<>();
        }

        if (mFileUtil == null) {
            mFileUtil = FileUtil.getInstance();
        }

        if (mFileListAdapter == null) {
            mFileListAdapter = new FileListAdapter(getActivity());
        }

        mExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        isFirstEnter = true;
        if (mInitDir == null) {
            String str[] = SharePreferenceUtils.getSavedFragmentState(mIndex);
            mInitDir = str[2];
            mBackStack.push(new Path("/", getString(R.string.root_dir)));
            String paths[] = str[1].split("/");
            String path = "";
            for (int i = 1; i < paths.length; i++) {
                path += "/" + paths[i];
                mBackStack.push(new Path(path, getPathName(path)));
            }
        } else {
            mBackStack.push(new Path(mInitDir, getPathName(mInitDir)));
        }
        mCurrentPath = mBackStack.peek();
        FileUtil.userSortMode = SharePreferenceUtils.getFileSortMode();

    }

    public String getPathName(String path) {
        String name[] = path.split("/");
        if (name.length <= 0) {
            return getString(R.string.root_dir);
        }
        if (mExternalStoragePath.equals(path)) {
            return getString(R.string.phone_storage);
        }
        return name[name.length - 1];
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.fragment_layout, null);
        mListView = (ListView) view.getChildAt(1);
        mTextViewPath = (TextView) ((FrameLayout) view.getChildAt(0)).getChildAt(0);

        if (isShowToUser) {
            mFileUtil.setOnLoadFileListener(loadFileListener);
            mFileUtil.listAllFile(mCurrentPath.getPath());
        }

        mListView.setAdapter(mFileListAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        showCurrentPathOnTextView(mCurrentPath.getPath());
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        getView().setVisibility(isVisibleToUser ? View.VISIBLE : View.GONE);
        isShowToUser = isVisibleToUser;
        if (isVisibleToUser && mFileUtil != null) {
            mFileUtil.setOnLoadFileListener(loadFileListener);
            if (mFileListAdapter.getList() == null) {
                mFileUtil.listAllFile(mCurrentPath.getPath());
            }
        }
    }

    /**
     * 判断当前的fragment是否对用户可见，即fragment是否显示在当前屏幕上
     * @return
     */
    public boolean isShowToUser() {
        return isShowToUser;
    }

    public void onCheckedChanged() {
        int size = "/".equals(mCurrentPath.getPath()) ? mFileListAdapter.getList().size() : mFileListAdapter.getList().size() -1;
        if ((mFileListAdapter.itemIsChecked() && mFileListAdapter.getCheckFileItem().size() < size)
        || !mFileListAdapter.itemIsChecked()) {
            mFileListAdapter.checkFileItem(true);
        } else {
            mFileListAdapter.checkFileItem(false);
        }
    }

    public void cancelCheckedItem() {
        mFileListAdapter.checkFileItem(false);
    }

    public void refresh() {
        mFileListAdapter.notifyDataSetChanged();
    }

    public void saveCurrentState() {
        int firstPosition = mListView.getFirstVisiblePosition();
        View view = mListView.getChildAt(0);
        int top = view == null ? 0 : view.getTop();
        SharePreferenceUtils.saveListViewFpAndTp(mIndex, firstPosition, top);
        SharePreferenceUtils.saveFragmentState(mIndex, mInitDir, mCurrentPath.getPath(), isShowToUser);
        if (mExternalStoragePath.equals(mInitDir)) {

        }
    }

    private void setTabTitle(String name) {
        if (mExternalStoragePath.equals(mCurrentPath.getPath())) {
            name = getString(R.string.phone_storage);
        }
        ((MainActivity)getActivity()).setTabTitle(mIndex, name);
    }

    private void handleFile(FileItem item) {
        switch (FileUtil.getFileType(item.getName())) {
            case FileUtil.FILE_IMAGE:
                ((MainActivity)getActivity()).prepareLookImage(item.getPath());
                break;
            case FileUtil.FILE_AUDIO:
                break;
            case FileUtil.FILE_VIDEO:
                break;
            case FileUtil.FILE_COMPRESS:
                break;
            case FileUtil.FILE_TEXT:
                break;
            case FileUtil.FILE_APK:
                //apk install
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.parse("file://" + item.getPath()),"application/vnd.android.package-archive");
                getActivity().startActivity(intent);
                break;
            case FileUtil.FILE_GIF:
                break;
        }
    }

    public void copy(Set<FileItem> items) {
        for (FileItem item : items) {
            mFileUtil.copy(item.getPath(), mCurrentPath.getPath());
        }
        mFileListAdapter.setItemOpera(false);
        cancelCheckedItem();
        mFileUtil.listAllFile(mCurrentPath.getPath());
    }

    public void cut(Set<FileItem> items) {
        for (FileItem item : items) {
            mFileUtil.cut(item.getPath(), mCurrentPath.getPath());
        }
        mFileListAdapter.setItemOpera(false);
        cancelCheckedItem();
        mFileUtil.listAllFile(mCurrentPath.getPath());
    }

    public void del(Set<FileItem> items) {
        for (FileItem item : items) {
            mFileUtil.del(item.getPath());
        }
        mFileListAdapter.setItemOpera(false);
        cancelCheckedItem();
        mFileUtil.listAllFile(mCurrentPath.getPath());
    }

    public void sort(int whichSort) {
        mFileUtil.sortFileItem(mFileListAdapter.getList(), whichSort);
        mFileListAdapter.notifyDataSetChanged();
    }

    public void operaItem(boolean opera, int action) {
        mFileListAdapter.setItemOpera(opera);
        switch (action) {
            case 1:
                //copy
                for (FileItem item : getCheckedItem()) {
                    mFileUtil.copy(item.getPath(), mCurrentPath.getPath());
                }
                mFileListAdapter.setItemOpera(false);
                cancelCheckedItem();
                mFileUtil.listAllFile(mCurrentPath.getPath());
                break;
            case 2:
                //cut
                break;
            case 3:
                //delete
                break;
            default:
                break;
        }
    }

    public Set<FileItem> getCheckedItem() {
        return mFileListAdapter.getCheckFileItem();
    }

    public boolean isItemOpera() {
        return mFileListAdapter.isItemOpera();
    }

    public void countDirSize(String path) {
        mFileUtil.countDirSize(path);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileItem item = (FileItem) mFileListAdapter.getItem(position);
        if (item.isUpper) {
            goBackUpperDir();
            return;
        }
        //如果文件被选中则进入文件选择模式
        if (mFileListAdapter.itemIsChecked() && !mFileListAdapter.isItemOpera()) {
            CheckBox box = (CheckBox) ((PercentRelativeLayout)view).getChildAt(5);
            if (item.isCheck()) {
                item.setCheck(false);
                box.setChecked(false);
            } else {
                item.setCheck(true);
                box.setChecked(true);
            }
            return;
        }
        //文件未被选中，如果是文件夹则进入
        String path = item.getPath();
        System.out.println(" -path--->" + path + "  isLink->" + item.isLink() + "  isFolder=" + item.isFolder() );
        if (item.isFolder()) {
            //文件夹
            showCurrentPathOnTextView(path);
            mFileListAdapter.setList(null);
            mFileUtil.listAllFile(path);
            mCurrentPath = mBackStack.push(new Path(path, item.getName()));
            setTabTitle(item.getName());
        } else {
            //文件
            handleFile(item);
        }

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        mLongSelFileItem = (FileItem) mFileListAdapter.getItem(position);
        if (mLongSelFileItem.isUpper) {
            return true;
        }
        if (mItemLongClickDialog == null) {
            initItemLongClickDialog();
        }

        mItemLongClickDialog.show();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.listview_item_longclick_permissionset:
                mItemLongClickDialog.dismiss();
                System.out.println("----->" + mLongSelFileItem.getPath());
                System.out.println("----->" + mLongSelFileItem.getPer());
                mPermiss = FileUtil.getFilePermissionNum(mLongSelFileItem.getPer());
                if (mPermissionSetDialog == null) {
                    Object objects[] = ViewManager.getInstance().createPermissionSetDialog(getActivity(), this, this);
                    mPermissionSetDialog = (AlertDialog) objects[0];
                    mPerCheckBoxs = ViewManager.getInstance().getCheckBoxArray();
                    int width = Integer.parseInt(objects[1].toString());
                    mPerSetFileName = (TextView) objects[2];
                    mPermissionOctalValueTextView = (TextView) objects[3];
                    preparePerAndCheckBox();
                    mPerSetFileName.setText(mLongSelFileItem.getName());
                    mPermissionSetDialog.show();
                    WindowManager.LayoutParams params = mPermissionSetDialog.getWindow().getAttributes();
                    params.width = width + 50;
                    mPermissionSetDialog.getWindow().setAttributes(params);
                    break;
                }
                preparePerAndCheckBox();
                mPerSetFileName.setText(mLongSelFileItem.getName());
                mPermissionSetDialog.show();
                break;
            case R.id.permission_cancel:
                mPermissionSetDialog.dismiss();
                break;
            case R.id.permission_confirm:
                break;
            case R.id.listview_item_longclick_property:
                TextView tv[] = ViewManager.getInstance().getPropertyTvArray();
                if (mFilePropertyDialog == null) {
                    mFilePropertyDialog = ViewManager.getInstance().createPropertyDialog(getActivity(), this);
                }
                tv[0].setText(getString(R.string.name_colon) + mLongSelFileItem.getName());
                tv[1].setText(getString(R.string.path_colon) + mLongSelFileItem.getPath());
                tv[2].setText(getString(R.string.perm_colon) + mLongSelFileItem.getPer());
                tv[4].setText(getString(R.string.time_colon) + TimeUtils.getFormatDateTime(mLongSelFileItem.lastModified()));
                tv[5].setText(getString(R.string.owner_colon) + mLongSelFileItem.getUser());
                tv[6].setText(getString(R.string.usergroup_colon) + mLongSelFileItem.getGroup());
                if (mLongSelFileItem.isLink()) {
                    tv[7].setVisibility(View.VISIBLE);
                    tv[7].setText(getString(R.string.linkto_colon) + mLongSelFileItem.linkTo());
                } else {
                    tv[7].setVisibility(View.GONE);
                }
                if (mLongSelFileItem.isFolder()) {
                    tv[3].setText(getString(R.string.size_colon).toString() + getString(R.string.counting));
                    countDirSize(mLongSelFileItem.getPath());
                } else {
                    tv[3].setText(getString(R.string.size_colon) + FileUtil.getFormatByte(mLongSelFileItem.size()));
                }
                mItemLongClickDialog.dismiss();
                mFilePropertyDialog.show();
                break;
            case R.id.property_confirm:
                mFilePropertyDialog.dismiss();
                break;
        }
    }

    /**
     * 处理后退键
     * @return
     */
    public boolean onKeyBack() {
        if (((MainActivity)getActivity()).isLookImage()) {
            ((MainActivity)getActivity()).stopLookImage();
            return true;
        }

        return goBackUpperDir();
    }

    private boolean goBackUpperDir() {
        if (mFileListAdapter.itemIsChecked() && !mFileListAdapter.isItemOpera()) {

            cancelCheckedItem();
            return true;
        }
        if (mBackStack.isEmpty() || (mExternalStoragePath.equals(mInitDir) && mCurrentPath.getPath().equals(mInitDir))) {
            return false;
        }
        mBackStack.pop();
        if (mBackStack.size() > 0) {
            mCurrentPath = mBackStack.peek();
            showCurrentPathOnTextView(mCurrentPath.getPath());
            setTabTitle(mCurrentPath.getName());
            mFileUtil.listAllFile(mCurrentPath.getPath());
            return true;
        }
        return false;
    }

    private void showCurrentPathOnTextView(String currentPath) {
        if ("/".equals(currentPath)) {
            mTextViewPath.setText(currentPath);
            return;
        }
        mTextViewPath.setText(currentPath + "/");
    }
    private FileUtil.OnLoadFileListener loadFileListener = new FileUtil.OnLoadFileListener() {
        @Override
        public void onLoadComplete(List<FileItem> items) {
            System.out.println("complete---file count--->" + items.size());
            if (!"/".equals(mCurrentPath.getPath())) {
                items.add(0, new FileItem(true));
            }
            mFileListAdapter.setList(items);
            if (isFirstEnter) {
                isFirstEnter = false;
                mListView.setSelectionFromTop(SharePreferenceUtils.getListViewFirstPos(mIndex), SharePreferenceUtils.getListViewTop(mIndex));
            }
        }

        @Override
        public void onLoadComplete(String str) {
            if (str != null) {
                long size = JSON.parseObject(str).getLongValue("totalSize");
                ViewManager.getInstance().getPropertyTvArray()[3].setText(getResources().getText(R.string.size_colon) + FileUtil.getFormatByte(size));
            }
        }

        @Override
        public void onError(String msg) {
            System.out.println("error-->" + msg);
        }
    };

    private void initItemLongClickDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ScrollView view = (ScrollView) getActivity().getLayoutInflater().inflate(R.layout.listview_item_longclick_menu, null);
        LinearLayout layout = (LinearLayout) view.getChildAt(0);
        for (int i = 1; i < layout.getChildCount(); i++) {
            if (layout.getChildAt(i) instanceof TextView)
                layout.getChildAt(i).setOnClickListener(this);
        }
        builder.setView(view);
        mItemLongClickDialog = builder.create();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.permission_set_checkowner_read:
                //0400 two
                owner = getPerNum(owner, 4, isChecked);
                break;
            case R.id.permission_set_checkusergroup_read:
                //0440 three
                userGroup = getPerNum(userGroup, 4, isChecked);
                break;
            case R.id.permission_set_checkother_read:
                //0444 four
                other = getPerNum(other, 4, isChecked);
                break;
            case R.id.permission_set_checkowner_write:
                // two
                owner = getPerNum(owner, 2, isChecked);
                break;
            case R.id.permission_set_checkusergroup_write:
                //three
                userGroup = getPerNum(userGroup, 2, isChecked);
                break;
            case R.id.permission_set_checkother_write:
                //four
                other = getPerNum(other, 2, isChecked);
                break;
            case R.id.permission_set_checkowner_execute:
                //two
                owner = getPerNum(owner, 1, isChecked);
                break;
            case R.id.permission_set_checkusergroup_execute:
                //three
                userGroup = getPerNum(userGroup, 1, isChecked);
                break;
            case R.id.permission_set_checkother_execute:
                //four
                other = getPerNum(other, 1, isChecked);
                break;
            case R.id.permission_set_check_setuid:
                //one
                perFlagSet = getPerNum(perFlagSet, 4, isChecked);
                break;
            case R.id.permission_set_check_setgid:
                perFlagSet = getPerNum(perFlagSet, 2, isChecked);
                //one
                break;
            case R.id.permission_set_check_setsticky:
                perFlagSet = getPerNum(perFlagSet, 1, isChecked);
                //one
                break;
            case R.id.permission_check_apply_childdirandfile:
                break;
            case R.id.permission_check_notapply_childfile:
                break;
        }
        mPermissionOctalValueTextView.setText("" + perFlagSet + owner + userGroup + other);
    }

    private int getPerNum(int perNum, int num, boolean isChecked) {
        if (isChecked) {
            perNum += num;
        } else {
            perNum -= num;
        }
        return perNum;
    }

    private void preparePerAndCheckBox() {
        setPerCheckBox(mPermiss[1], mPerCheckBoxs[0], mPerCheckBoxs[1], mPerCheckBoxs[2]);
        setPerCheckBox(mPermiss[2], mPerCheckBoxs[3], mPerCheckBoxs[4], mPerCheckBoxs[5]);
        setPerCheckBox(mPermiss[3], mPerCheckBoxs[6], mPerCheckBoxs[7], mPerCheckBoxs[8]);
        setPerCheckBox(mPermiss[0], mPerCheckBoxs[9], mPerCheckBoxs[10], mPerCheckBoxs[11]);
    }

    private void setPerCheckBox(int perNum, CheckBox cb1, CheckBox cb2, CheckBox cb3) {
        switch (perNum) {
            case 0:
                cb1.setChecked(false);
                cb2.setChecked(false);
                cb3.setChecked(false);
                break;
            case 1:
                cb3.setChecked(true);
                cb1.setChecked(false);
                cb2.setChecked(false);
                break;
            case 2:
                cb2.setChecked(true);
                cb1.setChecked(false);
                cb3.setChecked(false);
                break;
            case 3:
                cb2.setChecked(true);
                cb3.setChecked(true);
                cb1.setChecked(false);
                break;
            case 4:
                cb1.setChecked(true);
                cb2.setChecked(false);
                cb3.setChecked(false);
                break;
            case 5:
                cb1.setChecked(true);
                cb3.setChecked(true);
                cb2.setChecked(false);
                break;
            case 6:
                cb1.setChecked(true);
                cb2.setChecked(true);
                cb3.setChecked(false);
                break;
            case 7:
                cb1.setChecked(true);
                cb2.setChecked(true);
                cb3.setChecked(true);
                break;
        }
    }

    public void setRootDir(String rootDir) {
        this.mInitDir = rootDir;
    }

    public void setIndex(int mIndex) {
        this.mIndex = mIndex;
    }

    public int getIndex() {
        return mIndex;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        System.out.println(mIndex + " fragment onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println(mIndex + "fragment onDestroy");
    }
}
