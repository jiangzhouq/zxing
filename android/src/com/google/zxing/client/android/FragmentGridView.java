package com.google.zxing.client.android;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.zxing.client.android.history.Book;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class FragmentGridView extends Fragment {
	DisplayImageOptions options;
	private int numCol = 3;
	ArrayList<String> imageUrls = new ArrayList<String>();
	ImageLoader imageLoader;
	GridView grid_view ;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.bookstore_grid_view, container);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.ic_stub)
				.showImageForEmptyUri(R.drawable.ic_empty)
				.showImageOnFail(R.drawable.ic_error).cacheInMemory(true)
				.cacheOnDisk(true).considerExifParams(true)
				.bitmapConfig(Bitmap.Config.RGB_565).build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity())
		.threadPriority(Thread.NORM_PRIORITY - 2)
		.denyCacheImageMultipleSizesInMemory()
		.diskCacheFileNameGenerator(new Md5FileNameGenerator())
		.diskCacheSize(50 * 1024 * 1024) // 50 Mb
		.tasksProcessingOrder(QueueProcessingType.LIFO)
		.writeDebugLogs() // Remove for release app
		.build();
		imageLoader = ImageLoader.getInstance();
		imageLoader.init(config);
		Log.d("qiqi", "onActivityCreated");
		Cursor cur = getActivity().getContentResolver().query(
				Book.CONTENT_URI_BOOKS, new String[] { Book.COLUMN_IMAGE },
				null, null, null);
		cur.moveToFirst();
		for (int i = 0; i < cur.getCount(); i++) {
			imageUrls.add(cur.getString(0));
			cur.moveToNext();
		}
		grid_view = (GridView) getActivity().findViewById(R.id.grid_view);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			numCol = 4;
		} else {
			numCol = 3;
		}
		grid_view.setNumColumns(numCol);
		grid_view.setAdapter(new ImageAdapter());
		super.onActivityCreated(savedInstanceState);
	}

	static class ViewHolder {
		ImageView imageView;
		ProgressBar progressBar;
	}

	public class ImageAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return imageUrls.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			View view = convertView;
			if (view == null) {
				view = getActivity().getLayoutInflater().inflate(
						R.layout.item_grid_image, parent, false);
				holder = new ViewHolder();
				assert view != null;
				holder.imageView = (ImageView) view.findViewById(R.id.image);
				FrameLayout.LayoutParams imgvwDimens = new FrameLayout.LayoutParams(
						getActivity().getWindowManager().getDefaultDisplay()
								.getWidth()
								/ numCol, getActivity().getWindowManager()
								.getDefaultDisplay().getWidth()
								/ numCol);
				holder.imageView.setLayoutParams(imgvwDimens);
				holder.progressBar = (ProgressBar) view
						.findViewById(R.id.progress);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}

			imageLoader.displayImage(imageUrls.get(position),
					holder.imageView, options,
					new SimpleImageLoadingListener() {
						@Override
						public void onLoadingStarted(String imageUri, View view) {
							holder.progressBar.setProgress(0);
							holder.progressBar.setVisibility(View.VISIBLE);
						}

						@Override
						public void onLoadingFailed(String imageUri, View view,
								FailReason failReason) {
							holder.progressBar.setVisibility(View.GONE);
						}

						@Override
						public void onLoadingComplete(String imageUri,
								View view, Bitmap loadedImage) {
							holder.progressBar.setVisibility(View.GONE);
						}
					}, new ImageLoadingProgressListener() {
						@Override
						public void onProgressUpdate(String imageUri,
								View view, int current, int total) {
							holder.progressBar.setProgress(Math.round(100.0f
									* current / total));
						}
					});

			return view;
		}

	}
}
