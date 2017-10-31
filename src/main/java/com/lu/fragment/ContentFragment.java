package com.lu.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.lu.adapter.FileListAdapter;
import com.lu.filemanager2.MainActivity;
import com.lu.filemanager2.R;
import com.lu.model.FileItem;
import com.lu.utils.FileUtil;
import com.lu.utils.SharePreferenceUtils;

import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Created by lu on 2016/10/23.
 */

public class ContentFragment extends Fragment implements AdapterView.OnItemClickListener {
    private FileListAdapter mFileListAdapter;

    private Stack<String> mBackStack;

    private String currentPath = "/";

    private TextView mTextViewPath;

    private FileUtil fileUtil;

    private boolean isShowToUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mBackStack == null) {
            mBackStack = new Stack<>();
        }

        if (fileUtil == null) {
            fileUtil = FileUtil.getInstance();
        }

        if (mFileListAdapter == null) {
            mFileListAdapter = new FileListAdapter(getActivity());
        }

        FileUtil.userSortMode = SharePreferenceUtils.getFileSortMode();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_layout, null);
        ListView mListView = (ListView) view.findViewById(R.id.list_view);
        mTextViewPath = (TextView) view.findViewById(R.id.tv_current_path);

        if (isShowToUser) {
            fileUtil.setOnLoadFileListener(loadFileListener);
            fileUtil.listAllFile(currentPath);
        }

        if (mBackStack.size() < 1) {
            mBackStack.push(currentPath);
        }

        mListView.setAdapter(mFileListAdapter);
        mListView.setOnItemClickListener(this);

        mTextViewPath.setText(currentPath);
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isShowToUser = isVisibleToUser;
        if (isVisibleToUser && fileUtil != null) {
            fileUtil.setOnLoadFileListener(loadFileListener);
            if (mFileListAdapter.getList() == null) {
                fileUtil.listAllFile(currentPath);
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

    public void checkAllItem() {
        mFileListAdapter.checkFileItem(true);
    }

    public void cancelCheckedItem() {
        mFileListAdapter.checkFileItem(false);
    }

    public void refresh() {
        mFileListAdapter.notifyDataSetChanged();
    }

    public void copy(Set<FileItem> items) {
        for (FileItem item : items) {
            fileUtil.copy(item.getPath(), currentPath);
        }
        mFileListAdapter.setItemOpera(false);
        cancelCheckedItem();
        fileUtil.listAllFile(currentPath);
    }

    public void cut(Set<FileItem> items) {
        for (FileItem item : items) {
            fileUtil.cut(item.getPath(), currentPath);
        }
        mFileListAdapter.setItemOpera(false);
        cancelCheckedItem();
        fileUtil.listAllFile(currentPath);
    }

    public void del(Set<FileItem> items) {
        for (FileItem item : items) {
            fileUtil.del(item.getPath());
        }
        mFileListAdapter.setItemOpera(false);
        cancelCheckedItem();
        fileUtil.listAllFile(currentPath);
    }

    public void sort(int whichSort) {
        fileUtil.sortFileItem(mFileListAdapter.getList(), whichSort);
        mFileListAdapter.notifyDataSetChanged();
    }

    public void operaItem(boolean opera, int action) {
        mFileListAdapter.setItemOpera(opera);
        switch (action) {
            case 1:
                //copy
                for (FileItem item : getCheckedItem()) {
                    fileUtil.copy(item.getPath(), currentPath);
                }
                mFileListAdapter.setItemOpera(false);
                cancelCheckedItem();
                fileUtil.listAllFile(currentPath);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileItem item = (FileItem) mFileListAdapter.getItem(position);

        //如果文件被选中则进入文件选择模式
        if (mFileListAdapter.itemIsChecked() && !mFileListAdapter.isItemOpera()) {
            CheckBox box = (CheckBox) view.findViewById(R.id.checkbox_file);
            if (item.isCheck()) {
                item.setCheck(false);
                box.setChecked(false);
            } else {
                item.setCheck(true);
                box.setChecked(true);
            }
        } else {
            //文件未被选中，如果是文件夹则进入
            String path = item.getPath();
            System.out.println(" -path--->" + path + "  isLink->" + item.isLink() + "  isFolder=" + item.isFolder() );
            System.out.println();
            if (item.isFolder() || item.isLinkPath()) {
                //文件夹
                mFileListAdapter.setList(null);
                fileUtil.listAllFile(path);
                currentPath = path;
                mBackStack.push(currentPath);
                mTextViewPath.setText(currentPath);
            } else {
                //文件
            }

        }

    }

    /**
     * 处理后退键
     * @return
     */
    public boolean onKeyBack() {
        if (mFileListAdapter.itemIsChecked() && !mFileListAdapter.isItemOpera()) {
            cancelCheckedItem();
            return true;
        } else {
            if (mBackStack.isEmpty()) {
                return false;
            }
            mBackStack.pop();
            if (mBackStack.size() > 0) {
                currentPath = mBackStack.peek();
                mTextViewPath.setText(currentPath);
                fileUtil.listAllFile(currentPath);
                return true;
            }
        }
        return false;
    }

    private FileUtil.OnLoadFileListener loadFileListener = new FileUtil.OnLoadFileListener() {
        @Override
        public void onLoadComplete(List<FileItem> items) {
            mFileListAdapter.setList(items);
            System.out.println("complete---file count>" + items.size());
        }

        @Override
        public void onError(String msg) {
            System.out.println("error-->" + msg);
        }
    };


    @Override
    public void onDetach() {
        super.onDetach();
    }
}
