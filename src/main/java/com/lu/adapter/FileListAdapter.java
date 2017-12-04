package com.lu.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lu.filemanager2.MainActivity;
import com.lu.filemanager2.R;
import com.lu.filemanager2.databinding.ListViewFile2Binding;
import com.lu.model.FileItem;
import com.lu.utils.FileUtils;
import com.lu.utils.TimeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lu on 2016/10/29.
 */

public class FileListAdapter extends BasedAdapter<FileItem> implements CompoundButton.OnCheckedChangeListener {

    private ExecutorService mExecutorService;

    private static final String TAG = "FileListAdapter";

    private Set<FileItem> mCheckFileItem;
    private List<CheckBox> mCheckedBox;
    private List<CheckBox> mCheckBoxList;
    //private Set<String> mCheckItemPath;

    //private boolean isItemOpera;
    private boolean canRemoveCb = true;

    public FileListAdapter(Context context) {
        this.context = context;
        mCheckFileItem = new HashSet<>();
        mCheckedBox = new ArrayList<>();
        mCheckBoxList = new ArrayList<>();
        //mCheckItemPath = new HashSet<>();
        mLayoutInflater = LayoutInflater.from(context);
        mExecutorService = Executors.newFixedThreadPool(5);
    }

    @Override
    public void beforeSetList() {
        //mCheckBoxList.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileItem item = mList.get(position);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            ListViewFile2Binding viewBind = DataBindingUtil.inflate(mLayoutInflater, R.layout.list_view_file2, null, false);
            convertView = viewBind.getRoot();
            holder.fileIcon = viewBind.ivDirectory;
            holder.fileName = viewBind.tvFileName;
            holder.fileTime = viewBind.tvFileTime;
            holder.fileSize = viewBind.tvFileSize;
            holder.filePermiss = viewBind.tvFilePermission;
            holder.fileCheckBox = viewBind.checkboxFile;
            //checkbox设置是否选中状态监听和点击监听
            holder.fileCheckBox.setOnCheckedChangeListener(this);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //System.out.println(TAG + ": "+item.getAbsolutePath());
        /*if (isItemOpera) {
            holder.fileCheckBox.setVisibility(View.INVISIBLE);
        } else {
            holder.fileCheckBox.setVisibility(View.VISIBLE);
        }*/
        //mCheckBoxList.add(holder.fileCheckBox);
        holder.fileName.setText(item.getName());
        holder.fileSize.setText(item.formatSize());
        if (item.isUpper) {
            holder.fileIcon.setImageResource(item.getIcon());
            if (holder.fileCheckBox.getVisibility() == View.VISIBLE) {
                holder.fileCheckBox.setVisibility(View.GONE);
                holder.filePermiss.setVisibility(View.GONE);
                holder.fileTime.setVisibility(View.GONE);
            }
        } else {
            item.tvPermission = holder.filePermiss;
            if (holder.fileCheckBox.getVisibility() == View.GONE) {
                holder.fileCheckBox.setVisibility(View.VISIBLE);
                holder.filePermiss.setVisibility(View.VISIBLE);
                holder.fileTime.setVisibility(View.VISIBLE);
            }
            holder.fileCheckBox.setTag(position);
            holder.fileCheckBox.setChecked(item.isCheck());
            holder.fileTime.setText(TimeUtils.getFormatDateTime(item.lastModified()));
            holder.filePermiss.setText(item.getPer());

            //Glide.with(context).load(item.getIcon()).into(holder.fileIcon);

            switch (FileUtils.getFileType(item.getName())) {
                case FileUtils.FILE_IMAGE:
                    Glide.with(context).load(item.getPath())
                            .placeholder(R.drawable.ic_progress) //加载时的占位图
                            .error(R.drawable.image)
                            .into(holder.fileIcon);
                    break;
                case FileUtils.FILE_AUDIO:
                    holder.fileIcon.setImageResource(R.drawable.music);
                    break;
                case FileUtils.FILE_VIDEO:
                    Glide.with(context).load(Uri.fromFile(new File(item.getPath())))
                            .placeholder(R.drawable.video) //加载时的占位图
                            .error(R.drawable.video)
                            .into(holder.fileIcon);
                    break;
                case FileUtils.FILE_COMPRESS:
                    holder.fileIcon.setImageResource(R.drawable.compress);
                    break;
                case FileUtils.FILE_TEXT:
                case FileUtils.FILE_SCRIPT:
                    holder.fileIcon.setImageResource(R.drawable.text);
                    break;
                case FileUtils.FILE_APK:
                    //
                    holder.fileIcon.setImageResource(R.drawable.apk);
                    mExecutorService.execute(new LoadAPKIconRunnable(holder.fileIcon, item.getPath()));
                    break;
                case FileUtils.FILE_GIF:
                    Glide.with(context).load(item.getPath())
                            .asGif()
                            .placeholder(R.drawable.ic_progress) //加载时的占位图
                            .error(R.drawable.image)
                            .into(holder.fileIcon);
                    break;
                case FileUtils.FILE_OTHER:
                default:
                    holder.fileIcon.setImageResource(item.getIcon());
                    break;
            }
        }

        return convertView;
    }

    private void setFileTypeImage(int type) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        FileItem item = mList.get((Integer) buttonView.getTag());
        item.setCheck(isChecked);
        if (isChecked) {
            mCheckFileItem.add(item);
            mCheckedBox.add((CheckBox) buttonView);
        } else {
            if (mCheckFileItem.size() > 0) {
                mCheckFileItem.remove(item);
            }
            if (mCheckedBox.size() > 0 && canRemoveCb) {
                mCheckedBox.remove(buttonView);
            }
        }
        System.out.println("checkBox size-->" + mCheckedBox.size());
        System.out.println("mCheckFileItem size-->" + mCheckFileItem.size());
        ((MainActivity)context).onCheckBoxClick(itemIsChecked(), mCheckFileItem.size());
    }

    /**
     * 列表中的项目是否被选中
     * @return
     */
    public boolean itemIsChecked() {
        return mCheckFileItem.size() > 0 ? true : false;
    }

    public void checkFileItem(boolean isCheck) {
        if (isCheck){
            for (FileItem item : mList) {
                if (!item.isUpper) {
                    item.setCheck(isCheck);
                    mCheckFileItem.add(item);
                }
            }
            /*for (CheckBox box : mCheckBoxList) {
                box.setChecked(isCheck);
            }*/
            notifyDataSetChanged();
        } else {
            for (FileItem item : mCheckFileItem) {
                item.setCheck(isCheck);
            }
            mCheckFileItem.clear();
            canRemoveCb = false;
            for (CheckBox box : mCheckedBox) {
                box.setChecked(isCheck);
            }
            canRemoveCb = true;
            mCheckedBox.clear();
        }
        //notifyDataSetChanged();
    }

    public Set<FileItem> getCheckFileItem() {
        return mCheckFileItem;
    }

    public void addCheckFileItem(FileItem checkFileItem) {
        mCheckFileItem.add(checkFileItem);
    }

    class LoadAPKIconRunnable implements Runnable{
        ImageView imageView;
        String imgPath;
        public LoadAPKIconRunnable(ImageView imageView, String imgPath) {
            this.imageView = imageView;
            this.imgPath = imgPath;

        }
        @Override
        public void run() {
            PackageInfo apkInfo = context.getPackageManager().getPackageArchiveInfo(imgPath, PackageManager.GET_ACTIVITIES);
            if (apkInfo != null) {
                ApplicationInfo applicationInfo = apkInfo.applicationInfo;
                if (Build.VERSION.SDK_INT >= 8) {
                    applicationInfo.sourceDir = imgPath;
                    applicationInfo.publicSourceDir = imgPath;
                }
                Drawable drawable = context.getPackageManager().getApplicationIcon(applicationInfo);
                if (drawable != null) {
                    handler.obtainMessage(1, drawable).sendToTarget();
                }
            }
        }

        private Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    imageView.setImageDrawable((Drawable) msg.obj);
                }
            }
        };
    }

    class ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileTime;
        TextView fileSize;
        TextView filePermiss;
        CheckBox fileCheckBox;
    }
    public void release() {
        mExecutorService.shutdown();
    }

    public void clearCheckedBox() {
        for (CheckBox box : mCheckedBox) {
            box.setChecked(false);
        }
        mCheckedBox.clear();
    }
}
