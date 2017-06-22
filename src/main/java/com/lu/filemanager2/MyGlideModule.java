package com.lu.filemanager2;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.module.GlideModule;

/**
 * Created by bulefin on 2017/4/28.
 */

public class MyGlideModule implements GlideModule {

    private int memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
    private int diskCacheSize = 50 * (1 << 20);

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

        //配置缓存在内存中的内存大小
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, memoryCacheSize));
        //配置缓存在磁盘中的内存大小
        builder.setDiskCache(new ExternalCacheDiskCacheFactory(context, diskCacheSize));

        // 定义图片格式
       // builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
        // 默认
        //builder.setDecodeFormat(DecodeFormat.PREFER_RGB_565);
    }

    @Override
    public void registerComponents(Context context, Glide glide) {

    }
}
