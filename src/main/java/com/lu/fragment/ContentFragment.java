package com.lu.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lu.App;
import com.lu.adapter.FileListAdapter;
import com.lu.filemanager2.MainActivity;
import com.lu.filemanager2.R;
import com.lu.model.FileItem;
import com.lu.model.Item;
import com.lu.model.Path;
import com.lu.utils.FileUtil;
import com.lu.utils.PermissionUtils;
import com.lu.utils.SharePreferenceUtils;
import com.lu.utils.ShellUtil;
import com.lu.utils.TimeUtils;
import com.lu.utils.ToastUitls;
import com.lu.view.DialogManager;

import java.io.File;
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

    private boolean isBackKey;

    private Set<FileItem> mOperaItemSet;

    private String mFg = "-o";

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
        String savePath = mInitDir;
        if (mInitDir == null) {
            String str[] = SharePreferenceUtils.getSavedFragmentState(mIndex);
            savePath = str[1];
            mInitDir = str[2];
        }
        System.out.println(mIndex + "==>" + savePath);
        mBackStack.push(new Path("/", getString(R.string.root_dir)));
        String paths[] = savePath.split("/");
        String path = "";
        for (int i = 1; i < paths.length; i++) {
            path += "/" + paths[i];
            mBackStack.push(new Path(path, getPathName(path)));
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
        System.out.println("onCreateView");
        //mFileUtil.setOnLoadFileListener(loadFileListener);
        //mFileUtil.listAllFile(mCurrentPath.getPath());

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
                //mFileUtil.listAllFile("/");
                //mFileUtil.listAllFile("/");
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
        int checkSize = mFileListAdapter.getCheckFileItem().size();
        int totalSize = mFileListAdapter.getList().get(0).isUpper ? mFileListAdapter.getList().size() - 1 : mFileListAdapter.getList().size();
        //System.out.println("checksize "+ checkSize + "  totalsize=" + totalSize);
        if ((mFileListAdapter.itemIsChecked() && checkSize < totalSize )
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
        System.out.println("fragment save state start " + mIndex);
        if (mListView != null) {
            int firstPosition = mListView.getFirstVisiblePosition();
            View view = mListView.getChildAt(0);
            int top = view == null ? 0 : view.getTop();
            SharePreferenceUtils.saveListViewFpAndTp(mIndex, firstPosition, top);
            SharePreferenceUtils.saveFragmentState(mIndex, mInitDir, mCurrentPath.getPath(), isShowToUser);
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

    public void createDir(String name) {
        String cpath = mCurrentPath.getPath();
        cpath = "/".equals(cpath) ? cpath : cpath + "/";
        mFileUtil.createDir(cpath + name);
    }

    public void createFile(String name) {
        String cpath = mCurrentPath.getPath();
        cpath = "/".equals(cpath) ? cpath : cpath + "/";
        mFileUtil.createFile(cpath + name);
    }

    public void rename(String oldN, String newN) {
        String cpath = mCurrentPath.getPath();
        cpath = "/".equals(cpath) ? cpath : cpath + "/";
        if (oldN.equals(cpath + newN)) {
            return;
        }
        mFileUtil.rename(oldN, cpath + newN);
    }

    public void copy(Set<FileItem> items) {
        mOperaItemSet = items;
        for (FileItem item : items) {
            mFileUtil.copy(item.getPath(), mCurrentPath.getPath());
        }
        //cancelCheckedItem();
        //mFileUtil.listAllFile(mCurrentPath.getPath());
    }

    public void cut(Set<FileItem> items) {
        mOperaItemSet = items;
        for (FileItem item : items) {
            mFileUtil.cut(item.getPath(), mCurrentPath.getPath());
        }
        cancelCheckedItem();
        //mFileUtil.listAllFile(mCurrentPath.getPath());
    }

    public void del(Set<FileItem> items) {
        mOperaItemSet = items;
        for (FileItem item : items) {
            mFileUtil.del(item.getPath());
        }
        cancelCheckedItem();
        //mFileUtil.listAllFile(mCurrentPath.getPath());
    }

    public void sort(int whichSort) {
        mFileUtil.sortFileItem(mFileListAdapter.getList(), whichSort);
        mFileListAdapter.notifyDataSetChanged();
    }

    public void operaItem(boolean opera, int action) {
        cancelCheckedItem();
        switch (action) {
            case 1:
                //copy
                /*for (FileItem item : getCheckedItem()) {
                    mFileUtil.copy(item.getPath(), mCurrentPath.getPath());
                }*/
                //mFileUtil.listAllFile(mCurrentPath.getPath());
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

    public void countDirSize(String path) {
        mFileUtil.countDirSize(path);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileItem item = (FileItem) mFileListAdapter.getItem(position);
        if (item.isUpper) {
            isBackKey = false;
            goBackUpperDir();
            return;
        }
        //如果文件被选中则进入文件选择模式
        if (mFileListAdapter.itemIsChecked()) {
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

    private CheckBox mLongClickCheckBox;
    private View mLongClickSendTo;
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mFileListAdapter.itemIsChecked()) {
            return false;
        }
        mLongSelFileItem = (FileItem) mFileListAdapter.getItem(position);
        if (mLongSelFileItem.isUpper) {
            return true;
        }
        if (mItemLongClickDialog == null) {
            initItemLongClickDialog();
        }
        if (mLongSelFileItem.isFolder()) {
            mLongClickSendTo.setVisibility(View.GONE);
        } else {
            mLongClickSendTo.setVisibility(View.VISIBLE);
        }
        mLongClickCheckBox = (CheckBox) ((PercentRelativeLayout)view).getChildAt(5);
        mLongSelFileItem.setCheck(true);
        mLongClickCheckBox.setChecked(true);

        mItemLongClickDialog.show();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.listview_item_longclick_permissionset:
                if (ShellUtil.isGetRoot()) {
                    showPermissionSetDialog();
                } else {
                    mFileUtil.exeCommand("su\necho \"{'flag':'su','fg':'per','content':'$?'}\"");
                }
                break;
            case R.id.permission_cancel:
                mPermissionSetDialog.dismiss();
                break;
            case R.id.permission_confirm:
                String oldMode = "" + mPermiss[0] + mPermiss[1] + mPermiss[2] + mPermiss[3];
                String mode = "" + perFlagSet + owner + userGroup + other;
                //System.out.println(oldMode + "--permission_confirm:" + perFlagSet + owner + userGroup + other);
                mPermissionSetDialog.dismiss();
                if (!mLongSelFileItem.isFolder()) {
                    mFg = "-o";
                }
                if ("-o".equals(mFg) && mode.equals(oldMode)) {
                    return;
                }
                //System.out.println("permission_confirm:--> " + mFg);
                DialogManager.get().getDefaultProgress(getActivity(), "", getString(R.string.working_per)).show();
                mFileUtil.chmod(mLongSelFileItem.getPath(), mode, mFg);
                break;
            case R.id.listview_item_longclick_property:
                mItemLongClickDialog.dismiss();
                ((MainActivity)getActivity()).showProperty(mLongSelFileItem);
                break;
            case R.id.property_confirm:
                mFilePropertyDialog.dismiss();
                break;
            case R.id.listview_item_longclick_del:
                mItemLongClickDialog.dismiss();
                ((MainActivity)getActivity()).delete(mLongSelFileItem);
                break;
            case R.id.listview_item_longclick_copy:
                mItemLongClickDialog.dismiss();
                ((MainActivity)getActivity()).copy(mLongSelFileItem);
                break;
            case R.id.listview_item_longclick_move:
                mItemLongClickDialog.dismiss();
                ((MainActivity)getActivity()).move(mLongSelFileItem);
                break;
            case R.id.listview_item_longclick_rename:
                mItemLongClickDialog.dismiss();
                ((MainActivity)getActivity()).rename(mLongSelFileItem);
                break;
            case R.id.listview_item_longclick_sendto:
                mItemLongClickDialog.dismiss();
                Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mLongSelFileItem.getPath())));
                share.setType("*/*");//此处可发送多种文件
                //share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               // share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //sendIntent.setType("text/plain");
                //sendIntent.putExtra(Intent.EXTRA_TEXT, "这是一段分享的文字");
                startActivity(Intent.createChooser(share, "分享"));
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

        isBackKey = true;
        return goBackUpperDir();
    }

    private boolean goBackUpperDir() {
        if (mFileListAdapter.itemIsChecked()) {
            cancelCheckedItem();
            return true;
        }
        if (isBackKey && (mBackStack.isEmpty() || (mExternalStoragePath.equals(mInitDir) && mCurrentPath.getPath().equals(mInitDir)))) {
            return false;
        }
        if (mBackStack.isEmpty()) {
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
            if (items == null) {
                return;
            }
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

        }

        @Override
        public void onSizeComplete(String str) {
            long size = JSON.parseObject(str).getLongValue("totalSize");
            DialogManager.get().getPropertyTvArray()[3].setText(getResources().getText(R.string.size_colon) + FileUtil.getFormatByte(size));
        }

        @Override
        public void onRenameComplete(String str) {
            JSONObject jb = JSON.parseObject(str);
            if (jb.getBooleanValue("state")) {
                ToastUitls.showSMsg("重命名成功");
                mFileUtil.listAllFile(mCurrentPath.getPath());
            } else {
                ToastUitls.showSMsg("rename fail:" + jb.getString("reason"));
            }
        }

        @Override
        public void onCreateDirComplete(String str) {
            JSONObject jb = JSON.parseObject(str);
            if (jb.getBooleanValue("state")) {
                ToastUitls.showSMsg("文件夹创建成功");
                mFileUtil.listAllFile(mCurrentPath.getPath());
            } else {
                //ToastUitls.showSMsg("create dir fail:" + jb.getString("reason"));
                Object obj[] = DialogManager.get().getMsgDialog(getActivity(), (MainActivity)getActivity());
                if ("File exists".toLowerCase().contains(jb.getString("reason").toLowerCase())) {
                    ((TextView)obj[1]).setText("该名称已存在，文件夹创建失败");
                }
                ((AlertDialog)obj[0]).show();
            }
        }

        @Override
        public void onCreateFileComplete(String str) {
            System.out.println("onCreateFileComplete-->" + str);
            JSONObject jb = JSON.parseObject(str);
            if (jb.getBooleanValue("state")) {
                ToastUitls.showSMsg("文件创建成功");
                mFileUtil.listAllFile(mCurrentPath.getPath());
            } else {
                //ToastUitls.showSMsg("create file fail:" + jb.getString("reason"));
                Object obj[] = DialogManager.get().getMsgDialog(getActivity(), (MainActivity)getActivity());
                if ("File exists".toLowerCase().contains(jb.getString("reason").toLowerCase())) {
                    ((TextView)obj[1]).setText("该名称已存在，文件创建失败");
                }
                ((AlertDialog)obj[0]).show();
            }
        }

        Object progressObj[];
        long totalSize;
        long curenSize;
        @Override
        public void onCpAction(String str) {

            //mFileListAdapter.getList().add(mOperaItemSet.iterator().next());
            //mOperaItemSet.remove(mOperaItemSet.iterator().next());
            //mFileUtil.sortFileItem(mFileListAdapter.getList(), SharePreferenceUtils.getFileSortMode());
            //mFileListAdapter.notifyDataSetChanged();
            JSONObject cpJson = JSON.parseObject(str);
            if (cpJson.getBooleanValue("isOver")) {
                mFileUtil.listAllFile(mCurrentPath.getPath());
            } else {
                if (progressObj == null) {
                    progressObj = DialogManager.get().getProgressConfirmDialog(getActivity(), (MainActivity)getActivity());
                }
                totalSize = cpJson.getLongValue("totalSize");
                curenSize = cpJson.getLongValue("currentSize");
                System.out.println("totalSize:" + totalSize + "  curenSize:" + curenSize);
                if (totalSize > Integer.MAX_VALUE) {
                    ((ProgressBar)progressObj[5]).setMax(100000000);
                    ((ProgressBar)progressObj[5]).setProgress((int) (((float)curenSize / totalSize) * 100000000));
                } else {
                    ((ProgressBar)progressObj[5]).setMax((int) totalSize);
                    ((ProgressBar)progressObj[5]).setProgress((int) curenSize);
                }
                ((TextView)progressObj[2]).setText(getString(R.string.total_size) + "：" + FileUtil.getFormatByte(totalSize));
                ((TextView)progressObj[3]).setText(getString(R.string.copy_ed) + "：" + FileUtil.getFormatByte(curenSize));
                ((TextView)progressObj[4]).setText(getString(R.string.copy_ing) + "：" + cpJson.getString("name"));
                if (!((AlertDialog)progressObj[0]).isShowing()) {
                    ((AlertDialog)progressObj[0]).show();
                }
            }
        }

        @Override
        public void onMvComplete() {
            //mFileListAdapter.getList().add(mOperaItemSet.iterator().next());
            //mOperaItemSet.remove(mOperaItemSet.iterator().next());
            //mFileUtil.sortFileItem(mFileListAdapter.getList(), SharePreferenceUtils.getFileSortMode());
            //mFileListAdapter.notifyDataSetChanged();
            mFileUtil.listAllFile(mCurrentPath.getPath());
        }

        @Override
        public void onDelComplete() {
            for (FileItem item : mOperaItemSet) {
                mFileListAdapter.getList().remove(item);
            }
            //mFileUtil.sortFileItem(mFileListAdapter.getList(), SharePreferenceUtils.getFileSortMode());
            mFileListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onCHMAction(String string) {
            DialogManager.get().getDefaultProgress(getContext(), "", "").dismiss();
        }

        @Override
        public void onReqGetRoot(Item item) {
            System.out.println(item.fg + "--onReqGetRoot-->" + item.content);
            switch (item.fg) {
                case "per":
                    if ("0".equals(item.content)) {
                        showPermissionSetDialog();
                    } else {
                        Object obj[] = DialogManager.get().getMsgDialog(getActivity(), (MainActivity)getActivity());
                        ((TextView)obj[1]).setText("此功能仅对已经取得root权限的设备有效，应用获取root权限失败。");
                        ((AlertDialog)obj[0]).show();
                    }
                    break;
            }
        }

        @Override
        public void onError(String msg) {
            //System.out.println("onError-->" + msg);
        }
    };

    private void showPermissionSetDialog() {
        mItemLongClickDialog.dismiss();
        //System.out.println("----->" + mLongSelFileItem.getPath());
        //System.out.println("----->" + mLongSelFileItem.getPer());
        mPermiss = FileUtil.getFilePermissionNum(mLongSelFileItem.getPer());
        Object objects[] = DialogManager.get().createPermissionSetDialog(getActivity(), this, this);
        CheckBox dirFilecheckBox = (CheckBox) objects[4];
        CheckBox onlyFilecheckBox = (CheckBox) objects[5];
        if (mLongSelFileItem.isFolder()) {
            dirFilecheckBox.setVisibility(View.VISIBLE);
            onlyFilecheckBox.setVisibility(View.VISIBLE);
        } else {
            dirFilecheckBox.setVisibility(View.GONE);
            onlyFilecheckBox.setVisibility(View.GONE);
        }
        if (mPermissionSetDialog == null) {
            mPermissionSetDialog = (AlertDialog) objects[0];
            mPerCheckBoxs = DialogManager.get().getCheckBoxArray();
            int width = Integer.parseInt(objects[1].toString());
            mPerSetFileName = (TextView) objects[2];
            mPermissionOctalValueTextView = (TextView) objects[3];
            preparePerAndCheckBox();
            mPerSetFileName.setText(mLongSelFileItem.getName());
            mPermissionSetDialog.show();
            WindowManager.LayoutParams params = mPermissionSetDialog.getWindow().getAttributes();
            params.width = width + 50;
            mPermissionSetDialog.getWindow().setAttributes(params);
            return;
        }
        preparePerAndCheckBox();
        mPerSetFileName.setText(mLongSelFileItem.getName());
        mPermissionSetDialog.show();
    }

    private void initItemLongClickDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ScrollView view = (ScrollView) getActivity().getLayoutInflater().inflate(R.layout.listview_item_longclick_menu, null);
        LinearLayout layout = (LinearLayout) view.getChildAt(0);
        mLongClickSendTo = layout.getChildAt(14);
        for (int i = 1; i < layout.getChildCount(); i++) {
            if (layout.getChildAt(i) instanceof TextView)
                layout.getChildAt(i).setOnClickListener(this);
        }
        builder.setView(view);
        mItemLongClickDialog = builder.create();
        mItemLongClickDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mLongSelFileItem.setCheck(false);
                mLongClickCheckBox.setChecked(false);
            }
        });
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
                Object objects[] = DialogManager.get().createPermissionSetDialog(getActivity(), this, this);
                CheckBox checkBox = (CheckBox) objects[5];
                checkBox.setEnabled(isChecked ? true : false);
                if (isChecked) {
                    mFg = "-r";
                    checkBox.setChecked(false);
                    checkBox.setTextColor(getResources().getColor(R.color.per_setfont_color_enable));
                } else {
                    mFg = "-o";
                    checkBox.setTextColor(getResources().getColor(R.color.per_setfont_color_notable));
                }
                break;
            case R.id.permission_check_notapply_childfile:
                if (isChecked) {
                    mFg = "-d";
                } else {
                    mFg = "-r";
                }
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
