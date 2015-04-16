/*
 * Copyright 2013 David Schreiber
 *           2013 John Paul Nalog
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.google.zxing.client.android;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class FancyCoverFlowSampleAdapter extends FancyCoverFlowAdapter {

    // =============================================================================
    // Private members
    // =============================================================================

    private int[] images ;
    private String[] mImgs;
    // =============================================================================
    // Supertype overrides
    // =============================================================================

    DisplayImageOptions  options = new DisplayImageOptions.Builder().showStubImage(R.drawable.result_btn_bg)// 加载失败的时候
    		.cacheOnDisc().cacheInMemory().imageScaleType(ImageScaleType.EXACTLY_STRETCHED).build();
    ImageLoader imageLoader = ImageLoader.getInstance();
    public FancyCoverFlowSampleAdapter( Context context, String[] imgs) {
    	imageLoader.init(ImageLoaderConfiguration.createDefault(context));
    	mImgs = imgs;
	}
    @Override
    public int getCount() {
        return mImgs.length;
    }

    @Override
    public String getItem(int i) {
        return mImgs[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getCoverFlowItem(int i, View reuseableView, ViewGroup viewGroup) {
        ImageView imageView = null;

        if (reuseableView != null) {
            imageView = (ImageView) reuseableView;
        } else {
            imageView = new ImageView(viewGroup.getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setLayoutParams(new FancyCoverFlow.LayoutParams(300, 400));

        }
        imageLoader.displayImage(mImgs[i], imageView,options);
        return imageView;
    }
}
