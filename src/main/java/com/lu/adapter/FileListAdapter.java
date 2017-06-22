package com.lu.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import com.lu.model.FileItem;
import com.lu.utils.FileUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lu on 2016/10/29.
 */

public class FileListAdapter extends BasedAdapter<FileItem> implements CompoundButton.OnCheckedChangeListener {

    private ExecutorService mExecutorService;

    private static final String TAG = "FileListAdapter";

    public FileListAdapter(Context context) {
        this.context = context;
        mLayoutInflater = LayoutInflater.from(context);
        mExecutorService = Executors.newFixedThreadPool(5);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileItem item = mList.get(position);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.list_view_file, null);
            holder.fileIcon = (ImageView) convertView.findViewById(R.id.iv_directory);
            holder.fileName = (TextView) convertView.findViewById(R.id.tv_file_name);
            holder.fileTime = (TextView) convertView.findViewById(R.id.tv_file_time);
            holder.fileSize = (TextView) convertView.findViewById(R.id.tv_file_size);
            holder.fileCheckBox = (CheckBox) convertView.findViewById(R.id.checkbox_file);
            //checkbox设置是否选中状态监听和点击监听
            holder.fileCheckBox.setOnCheckedChangeListener(this);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //System.out.println(TAG + ": "+item.getAbsolutePath());
        holder.fileCheckBox.setTag(position);
        holder.fileCheckBox.setChecked(item.isCheck());
        holder.fileName.setText(item.getName());
        holder.fileSize.setText(item.getSize());
        holder.fileTime.setText(item.getDate());
        Glide.with(context).load(item.getIcon()).into(holder.fileIcon);

        switch (item.getType()) {
            case FileUtil.FILE_IMAGE:
                Glide.with(context).load(item.getPath())
                        .placeholder(R.drawable.ic_progress) //加载时的占位图
                        .error(R.drawable.image)
                        .into(holder.fileIcon);
                break;
            case FileUtil.FILE_AUDIO:
                Glide.with(context).load(R.drawable.music)
                        .into(holder.fileIcon);
                break;
            case FileUtil.FILE_VIDEO:
                Glide.with(context).load(Uri.fromFile(new File(item.getPath())))
                        .placeholder(R.drawable.video) //加载时的占位图
                        .error(R.drawable.video)
                        .into(holder.fileIcon);
                break;
            case FileUtil.FILE_COMPRESS:

                break;
            case FileUtil.FILE_TEXT:
                Glide.with(context).load(R.drawable.text)
                        .into(holder.fileIcon);
                break;
            case FileUtil.FILE_APK:
                //
               mExecutorService.execute(new LoadAPKIconRunnable(holder.fileIcon, item.getPath()));
                break;
            case FileUtil.FILE_GIF:
                Glide.with(context).load(item.getPath())
                        .asGif()
                        .placeholder(R.drawable.ic_progress) //加载时的占位图
                        .error(R.drawable.image)
                        .into(holder.fileIcon);
                break;
        }

        return convertView;
    }

    private void setFileTypeImage(int type) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mList.get((Integer) buttonView.getTag()).setCheck(isChecked);
        ((MainActivity)context).onCheckBoxClick(itemIsChecked());
    }

    /**
     * 列表中的项目是否被选中
     * @return
     */
    public boolean itemIsChecked() {
        boolean isCheck = false;
        for (FileItem checkItem : mList) {
            if (checkItem.isCheck()) {
                isCheck = true;
                break;
            }
        }
        return isCheck;
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

    public void release() {
        mExecutorService.shutdown();
    }

    class ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileTime;
        TextView fileSize;
        CheckBox fileCheckBox;
    }
}
