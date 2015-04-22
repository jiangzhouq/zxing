package com.google.zxing.client.android;

import com.google.zxing.client.android.history.Book;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;

public class FragmentFlowView extends Fragment{
	private FancyCoverFlow fancyCoverFlow;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.bookstore_flow_view, container);
		
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
			Log.d("qiqi", "onActivityCreated");
		 	Cursor cur = getActivity().getContentResolver().query(Book.CONTENT_URI_BOOKS, new String[]{Book.COLUMN_IMAGE}, null, null, null);
	        String[] imgs = new String[cur.getCount()];
	        cur.moveToFirst();
	        for(int i = 0; i < cur.getCount(); i++){
	        	imgs[i] = cur.getString(0);
	        	Log.d("qiqi", cur.getString(0));
	        	cur.moveToNext();
	        }
	        this.fancyCoverFlow = (FancyCoverFlow) getActivity().findViewById(R.id.fancyCoverFlow);
	        this.fancyCoverFlow.setAdapter(new FancyCoverFlowSampleAdapter(getActivity(), imgs));
	        this.fancyCoverFlow.setUnselectedAlpha(0.2f);
	        this.fancyCoverFlow.setUnselectedSaturation(0.0f);
	        this.fancyCoverFlow.setUnselectedScale(0.2f);
	        this.fancyCoverFlow.setSpacing(100);
	        this.fancyCoverFlow.setMaxRotation(0);
	        this.fancyCoverFlow.setScaleDownGravity(0.2f);
	        this.fancyCoverFlow.setActionDistance(FancyCoverFlow.ACTION_DISTANCE_AUTO);
		super.onActivityCreated(savedInstanceState);
	}
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
	}
}
