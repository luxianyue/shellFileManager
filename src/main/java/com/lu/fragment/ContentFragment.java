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
import android.util.TypedValue;
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
import com.alibaba.fastjson.JSONObject;
import com.lu.App;
import com.lu.activity.TextActivity;
import com.lu.adapter.FileListAdapter;
import com.lu.filemanager2.MainActivity;
import com.lu.filemanager2.R;
import com.lu.model.FileItem;
import com.lu.model.Item;
import com.lu.model.Path;
import com.lu.utils.FileUtils;
import com.lu.utils.SharePreferenceUtils;
import com.lu.utils.ShellUtil;
import com.lu.utils.ToastUitls;
import com.lu.view.DialogManager;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Created by lu on 2016/10/23.
 */

public class ContentFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private ListView mListView;
    private FileListAdapter mAdapter;

    private Stack<Path> mBackStack;

    private Path mCurrentPath;

    private TextView mTextViewPath;

    private FileUtils mFileUtils;

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

    private Map<String, int[]> mPositionMap;

    private int mUpdateIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mBackStack == null) {
            mBackStack = new Stack<>();
        }

        if (mFileUtils == null) {
            mFileUtils = FileUtils.get();
        }

        if (mAdapter == null) {
            mAdapter = new FileListAdapter(getActivity());
        }

        mPositionMap = new HashMap<>();

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
        int array[] = {SharePreferenceUtils.getListViewFirstPos(mIndex), SharePreferenceUtils.getListViewTop(mIndex)};
        mPositionMap.put(mCurrentPath.getPath(), array);
        FileUtils.userSortMode = SharePreferenceUtils.getFileSortMode();

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
        //mFileUtils.setOnLoadFileListener(loadFileListener);
        //mFileUtils.listAllFile(mCurrentPath.getPath());

        mListView.setAdapter(mAdapter);
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
       if (isVisibleToUser && mFileUtils != null) {
            mFileUtils.setOnLoadFileListener(loadFileListener);
            if (mAdapter.getList() == null) {
                mFileUtils.listAllFile(mCurrentPath.getPath());
                //mFileUtils.listAllFile("/");
                //mFileUtils.listAllFile("/");
                //String str = "";
                //FileUtils.checkString(str);
                //mFileUtils.exeCommand(App.tools + " " + str);
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
        int checkSize = mAdapter.getCheckFileItem().size();
        int totalSize = mAdapter.getList().get(0).isUpper ? mAdapter.getList().size() - 1 : mAdapter.getList().size();
        //System.out.println("checksize "+ checkSize + "  totalsize=" + totalSize);
        if ((mAdapter.itemIsChecked() && checkSize < totalSize )
        || !mAdapter.itemIsChecked()) {
            mAdapter.checkFileItem(true);
        } else {
            mAdapter.checkFileItem(false);
        }
    }

    public void cancelCheckedItem() {
        mAdapter.checkFileItem(false);
    }

    public void refresh() {
        mAdapter.notifyDataSetChanged();
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
        switch (FileUtils.getFileType(item.getName())) {
            case FileUtils.FILE_IMAGE:
                ((MainActivity)getActivity()).prepareLookImage(item.getPath());
                break;
            case FileUtils.FILE_AUDIO:
                MainActivity.isWhat = MainActivity.MEDIA_AUDIO;
            case FileUtils.FILE_VIDEO:
                Object mediaObj[] = DialogManager.get().getMsgConfirmDialog(getActivity(), (MainActivity)getActivity(), 4);
                ((TextView)mediaObj[1]).setText(getString(R.string.tip_media));
                ((TextView)mediaObj[2]).setText(getString(R.string.tip_media_content));
                if (MainActivity.isWhat == 0) {
                    MainActivity.isWhat = MainActivity.MEDIA_VIDEO;
                }
                ((TextView)mediaObj[4]).setTag(item.getPath());
                ((TextView)mediaObj[5]).setTag(item.getPath());
                ((AlertDialog)mediaObj[0]).show();
                break;
            case FileUtils.FILE_COMPRESS:
                break;
            case FileUtils.FILE_TEXT:
                //mFileUtils.do_text(item.getPath(), App.tempFilePath, 'l');
                Object msgObj[] = DialogManager.get().getMsgConfirmDialog(getActivity(), (MainActivity)getActivity(), 3);
                ((TextView)msgObj[1]).setText(getString(R.string.tip_text));
                ((TextView)msgObj[2]).setText(getString(R.string.tip_text_content));
                MainActivity.isWhat = MainActivity.LOOK_EDIT;
                ((TextView)msgObj[5]).setTag(item.getPath());
                ((AlertDialog)msgObj[0]).show();
                break;
            case FileUtils.FILE_SCRIPT:
                //mFileUtils.do_text(item.getPath(), App.tempFilePath, 'l');
                Object smsgObj[] = DialogManager.get().getMsgConfirmDialog(getActivity(), (MainActivity)getActivity(), 1);
                ((TextView)smsgObj[1]).setText(getString(R.string.tip_script));
                ((TextView)smsgObj[2]).setText(getString(R.string.tip_script_content));
                MainActivity.isWhat = MainActivity.LOOK_EDIT;
                ((TextView)smsgObj[4]).setTag(item.getPath());
                ((TextView)smsgObj[5]).setTag(item.getPath());
                ((AlertDialog)smsgObj[0]).show();
                break;
            case FileUtils.FILE_APK:
                //apk install
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.parse("file://" + item.getPath()),"application/vnd.android.package-archive");
                getActivity().startActivity(intent);
                break;
            case FileUtils.FILE_GIF:
                break;
        }
    }

    public void createDir(String name) {
        String cpath = mCurrentPath.getPath();
        cpath = "/".equals(cpath) ? cpath : cpath + "/";
        mFileUtils.createDir(cpath + name);
    }

    public void createFile(String name) {
        String cpath = mCurrentPath.getPath();
        cpath = "/".equals(cpath) ? cpath : cpath + "/";
        mFileUtils.createFile(cpath + name);
    }

    public void rename(String oldN, String newN, int position) {
        mUpdateIndex = position;
        String cpath = mCurrentPath.getPath();
        cpath = "/".equals(cpath) ? cpath : cpath + "/";
        if (oldN.equals(cpath + newN)) {
            return;
        }
        System.out.println("oldName-->" + oldN + "  -->" + newN);
        mFileUtils.rename(oldN,cpath + newN);
    }

    public void copy(Set<FileItem> items) {
        mOperaItemSet = items;
        String srcPath = "";
        for (FileItem item : items) {
            srcPath = srcPath + " " + FileUtils.getS(FileUtils.checkString(item.getPath()));
        }
        mFileUtils.copy(mCurrentPath.path(), srcPath);
    }

    public void cut(Set<FileItem> items) {
        mOperaItemSet = items;
        String srcPath = "";
        for (FileItem item : items) {
            srcPath = srcPath + " " + FileUtils.getS(FileUtils.checkString(item.getPath()));
        }
        mFileUtils.cut(mCurrentPath.path(), srcPath);
        //cancelCheckedItem();
        //mFileUtils.listAllFile(mCurrentPath.getPath());
    }

    public void del(Set<FileItem> items) {
        mOperaItemSet = items;
        String srcPath = "";
        for (FileItem item : items) {
            srcPath = srcPath + " " + FileUtils.getS(FileUtils.checkString(item.getPath()));
        }
        mFileUtils.del(srcPath);
        //cancelCheckedItem();
        //mFileUtils.listAllFile(mCurrentPath.getPath());
    }

    public void chmod(Set<FileItem> set, String mode, String mfg) {
        String srcPath = "";
        for (FileItem item : set) {
            srcPath = srcPath + " " + FileUtils.getS(FileUtils.checkString(item.getPath()));
        }
        mFileUtils.chmod(srcPath, mode, mfg);
    }

    public void sort(int whichSort) {
        if (mAdapter.getList() == null || mAdapter.getList().size() <= 0) {
            return;
        }
        mFileUtils.sortFileItem(mAdapter.getList(), whichSort);
        mAdapter.notifyDataSetChanged();
    }

    private void updateView(int itemIndex) {
        //得到第一个可显示控件的位置，
        int visiblePosition = mListView.getFirstVisiblePosition();
        //只有当要更新的view在可见的位置时才更新，不可见时，跳过不更新
        if (itemIndex - visiblePosition >= 0) {
            //得到要更新的item的view
            View view = mListView.getChildAt(itemIndex - visiblePosition);
            //调用adapter更新界面
            mAdapter.updateView(view, itemIndex);
        }
    }

    public Set<FileItem> getCheckedItem() {
        return mAdapter.getCheckFileItem();
    }

    public void countDirSize(String path) {
        mFileUtils.countDirSize(path);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileItem item = (FileItem) mAdapter.getItem(position);
        if (item.isUpper) {
            isBackKey = false;
            goBackUpperDir();
            return;
        }
        //如果文件被选中则进入文件选择模式
        if (mAdapter.itemIsChecked()) {
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
            mAdapter.setList(null);
            int array[] = {mListView.getFirstVisiblePosition(), mListView.getChildAt(0).getTop()};
            mPositionMap.put(mCurrentPath.getPath(), array);
            mFileUtils.listAllFile(path);
            mCurrentPath = mBackStack.push(new Path(path, item.getName()));
            setTabTitle(item.getName());
        } else {
            //文件
            handleFile(item);
        }

    }

    private CheckBox mLongClickCheckBox;
    private View mLongClickSendTo;
    private View mLongClickOpenWay;
    private View mLongClickLookOrEdit;
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter.itemIsChecked()) {
            return false;
        }
        mLongSelFileItem = (FileItem) mAdapter.getItem(position);
        if (mLongSelFileItem.isUpper) {
            return true;
        }
        if (mItemLongClickDialog == null) {
            initItemLongClickDialog();
        }
        if (mLongSelFileItem.isFolder()) {
            mLongClickSendTo.setVisibility(View.GONE);
            mLongClickOpenWay.setVisibility(View.GONE);
            mLongClickLookOrEdit.setVisibility(View.GONE);
            ((View)mLongClickSendTo.getTag()).setVisibility(View.GONE);
            ((View)mLongClickOpenWay.getTag()).setVisibility(View.GONE);
            ((View)mLongClickLookOrEdit.getTag()).setVisibility(View.GONE);
        } else {
            mLongClickSendTo.setVisibility(View.VISIBLE);
            mLongClickOpenWay.setVisibility(View.VISIBLE);
            mLongClickLookOrEdit.setVisibility(View.VISIBLE);
            ((View)mLongClickSendTo.getTag()).setVisibility(View.VISIBLE);
            ((View)mLongClickOpenWay.getTag()).setVisibility(View.VISIBLE);
            ((View)mLongClickLookOrEdit.getTag()).setVisibility(View.VISIBLE);
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
                showPermissionSetDialog();
                if (ShellUtil.isGetRoot()) {
                } else {
                    //mFileUtils.exeCommand("su\necho \"{\\\"flag\\\":\\\"su\\\",\\\"fg\\\":\\\"per\\\",\\\"content\\\":\\\"$?\\\"}\"");
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
                //DialogManager.get().getDefaultProgress(getActivity(), "", getString(R.string.working_per)).show();
                mFileUtils.chmod(" " + FileUtils.getS(FileUtils.checkString(mLongSelFileItem.getPath())), mode, mFg);
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
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + mLongSelFileItem.getPath()));
                share.setType("*/*");//此处可发送多种文件
                //share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               // share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //sendIntent.setType("text/plain");
                //sendIntent.putExtra(Intent.EXTRA_TEXT, "这是一段分享的文字");
                startActivity(Intent.createChooser(share, "分享"));
                break;
            case R.id.listview_item_longclick_lookoredit:
                mItemLongClickDialog.dismiss();
                FileUtils.get().do_text(mLongSelFileItem.getPath(), App.tempFilePath, 'l');
                break;
            case R.id.listview_item_longclick_openway:
                //file open way
                mItemLongClickDialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + mLongSelFileItem.getPath()), "*/*");
                startActivity(intent);
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
        if (mAdapter.itemIsChecked()) {
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
        mPositionMap.remove(mCurrentPath.getPath());
        if (mBackStack.size() > 0) {
            mCurrentPath = mBackStack.peek();
            showCurrentPathOnTextView(mCurrentPath.getPath());
            setTabTitle(mCurrentPath.getName());
            mFileUtils.listAllFile(mCurrentPath.getPath());
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
    private FileUtils.OnLoadFileListener loadFileListener = new FileUtils.OnLoadFileListener() {
        @Override
        public void onLoadComplete(List<FileItem> items) {
            if (items == null) {
                return;
            }
            System.out.println("complete---file count--->" + items.size());
            if (!"/".equals(mCurrentPath.getPath())) {
                items.add(0, new FileItem(true));
            }
            mAdapter.setList(items);
            int array[] = mPositionMap.get(mCurrentPath.getPath());
            if (array != null)
                mListView.setSelectionFromTop(array[0], array[1]);
        }

        @Override
        public void onLoadComplete(String str) {
        }

        @Override
        public void onSizeComplete(String str) {
            long size = JSON.parseObject(str).getLongValue("totalSize");
            DialogManager.get().getPropertyTvArray()[3].setText(getResources().getText(R.string.size_colon) + FileUtils.getFormatByte(size));
        }

        @Override
        public void onRenameComplete(String str) {
            JSONObject jb = JSON.parseObject(str);
            if (jb.getBooleanValue("state")) {
                ToastUitls.showSMsg("重命名成功");
                //mFileUtils.listAllFile(mCurrentPath.getPath());
                String path = jb.getString("path");
                mAdapter.getList().get(mUpdateIndex).p = path;
                mAdapter.getList().get(mUpdateIndex).n = path.substring(path.lastIndexOf("/") + 1);
                updateView(mUpdateIndex);
            } else {
                ToastUitls.showSMsg("rename fail:" + jb.getString("reason"));
            }
        }

        @Override
        public void onCreateDirComplete(String str) {
            JSONObject jb = JSON.parseObject(str);
            if (jb.getBooleanValue("state")) {
                ToastUitls.showSMsg("文件夹创建成功");
                mFileUtils.listAllFile(mCurrentPath.getPath());
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
                mFileUtils.listAllFile(mCurrentPath.getPath());
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

            //mAdapter.getList().add(mOperaItemSet.iterator().next());
            //mOperaItemSet.remove(mOperaItemSet.iterator().next());
            //mFileUtils.sortFileItem(mAdapter.getList(), SharePreferenceUtils.getFileSortMode());
            //mAdapter.notifyDataSetChanged();
            JSONObject cpJson = JSON.parseObject(str);
            if (cpJson.getBooleanValue("isOver")) {
                mFileUtils.listAllFile(mCurrentPath.getPath());
                ToastUitls.showSMsg("文件复制完成");
            } else {
                /*if (progressObj == null) {
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
                ((TextView)progressObj[2]).setText(getString(R.string.total_size) + "：" + FileUtils.getFormatByte(totalSize));
                ((TextView)progressObj[3]).setText(getString(R.string.copy_ed) + "：" + FileUtils.getFormatByte(curenSize));
                ((TextView)progressObj[4]).setText(getString(R.string.copy_ing) + "：" + cpJson.getString("name"));
                if (!((AlertDialog)progressObj[0]).isShowing()) {
                    ((AlertDialog)progressObj[0]).show();
                }*/
            }
        }

        @Override
        public void onMvAction(String str) {
            Item item = JSON.parseObject(str, Item.class);
            if (item.isOver) {
                mFileUtils.listAllFile(mCurrentPath.getPath());
                ToastUitls.showSMsg("文件移动完成");
            }
        }

        @Override
        public void onDelAction(String str) {
            Item item = JSON.parseObject(str, Item.class);
            if (item.isOver) {
                for (FileItem fItem : mOperaItemSet) {
                    mAdapter.getList().remove(fItem.position());
                }
                ToastUitls.showSMsg("文件删除完成");
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onCHMAction(String str) {
            DialogManager.get().getDefaultProgress(getContext(), "", "").dismiss();
            Item item = JSON.parseObject(str, Item.class);
            String per = FileUtils.getPermissionByMode(item.mode);
            if (item.state) {
                mLongSelFileItem.tp = mLongSelFileItem.tp.substring(0, 2) + per;
                mLongSelFileItem.tvPermission.setText(mLongSelFileItem.getPer());
                System.out.println(mLongSelFileItem.tp.substring(0, 2));
            }
            System.out.println("fragment-onCHMAction------>"+str);
        }

        @Override
        public void onTextAction(String str) {
            Intent activityIntent = new Intent(getContext(), TextActivity.class);
            activityIntent.putExtra("path", JSON.parseObject(str).getString("path"));
            startActivity(activityIntent);
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
        mPermiss = FileUtils.getFilePermissionNum(mLongSelFileItem.getPer());
        Object objects[] = DialogManager.get().createPermissionSetDialog(getActivity(), this, this);
        CheckBox dirFilecheckBox = (CheckBox) objects[4];
        CheckBox onlyFilecheckBox = (CheckBox) objects[5];
        dirFilecheckBox.setChecked(false);
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
            params.width = width + (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, getResources().getDisplayMetrics());
            mPermissionSetDialog.getWindow().setAttributes(params);
            return;
        }
        preparePerAndCheckBox();
        mPerSetFileName.setText(mLongSelFileItem.getName());
        mPermissionSetDialog.show();
    }

    public void showItemLongClickDialog() {
    }

    private void initItemLongClickDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ScrollView view = (ScrollView) getActivity().getLayoutInflater().inflate(R.layout.listview_item_longclick_menu, null);
        LinearLayout layout = (LinearLayout) view.getChildAt(0);
        mLongClickSendTo = layout.getChildAt(16);
        mLongClickSendTo.setTag(layout.getChildAt(15));
        mLongClickOpenWay = layout.getChildAt(2);
        mLongClickOpenWay.setTag(layout.getChildAt(3));
        mLongClickLookOrEdit = layout.getChildAt(18);
        mLongClickLookOrEdit.setTag(layout.getChildAt(17));
        for (int i = 1; i < layout.getChildCount(); i++) {
            if (layout.getChildAt(i) instanceof TextView) {
                layout.getChildAt(i).setOnClickListener(this);
            }
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
