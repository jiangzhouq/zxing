/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.result;

import java.security.PublicKey;
import java.util.HashMap;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.history.Book;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
/**
 * Handles books encoded by their ISBN values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ISBNResultHandler extends ResultHandler {
	public static final int INSERT_ADD_SUCCESS = 1;
	public static final int INSERT_ADD_FAILED = 2;
	public static final int INSERT_WANT_SUCCESS = 3;
	public static final int INSERT_WANT_FAILED = 4;
	public static final int INSERT_ADDED = 5;
	public static final int INSERT_WANTED = 6;
	
  private static final int[] buttons = {
      R.string.button_product_search,
      R.string.button_book_search,
      R.string.button_search_book_contents,
      R.string.button_custom_product_search
  };
  private Activity mActivity;
  private static final String STRING_DOUBAN_URL = "https://api.douban.com/v2/book/isbn/:";
  AsyncHttpClient client = new AsyncHttpClient();
  private  String result = "";
  @Override
	public void handleAuto(String isbn) {
	  String wholeUrl = STRING_DOUBAN_URL + isbn;
	  Log.d("qiqi", wholeUrl);
	  result = "";
	  client.get(wholeUrl, new DoubanJsonHandler(){});
	  Log.d("qiqi", "return  " + result);
	}
  class DoubanJsonHandler extends JsonHttpResponseHandler{
	  @Override
	public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
		// TODO Auto-generated method stub
			Book mBook = createBookFromResponse(response);
			((CaptureActivity)mActivity).doubanComplete(mBook, ISBNResultHandler.this);
	}
  }
  private Book createBookFromResponse(JSONObject response){
	  Book mBook = new Book();
	  try{
		  mBook.mDoubanId = response.isNull(Book.COLUMN_DOUBAN_ID)?"":response.getString(Book.COLUMN_DOUBAN_ID);
		  mBook.mISBN10 = response.isNull(Book.COLUMN_ISBN10)?"":response.getString(Book.COLUMN_ISBN10);
		  mBook.mISBN13 = response.isNull(Book.COLUMN_ISBN13)?"":response.getString(Book.COLUMN_ISBN13);
		  mBook.mTitle = response.isNull(Book.COLUMN_TITLE)?"":response.getString(Book.COLUMN_TITLE);
		  mBook.mOriginTitle = response.isNull(Book.COLUMN_ORIGIN_TITLE)?"":response.getString(Book.COLUMN_ORIGIN_TITLE);
		  mBook.mAltTitle = response.isNull(Book.COLUMN_ALT_TITLE)?"":response.getString(Book.COLUMN_ALT_TITLE);
		  mBook.mSubTitle = response.isNull(Book.COLUMN_SUB_TITLE)?"":response.getString(Book.COLUMN_SUB_TITLE);
		  mBook.mUrl = response.isNull(Book.COLUMN_URL)?"":response.getString(Book.COLUMN_URL);
		  mBook.mAlt = response.isNull(Book.COLUMN_ALT)?"":response.getString(Book.COLUMN_ALT);
		  mBook.mImage = response.isNull(Book.COLUMN_IMAGE)?"":response.getString(Book.COLUMN_IMAGE);
		  mBook.mImages = response.isNull(Book.COLUMN_IMAGES)?"":response.getString(Book.COLUMN_IMAGES);
		  mBook.mAuthor = response.isNull(Book.COLUMN_AUTHOR)?"":response.getString(Book.COLUMN_AUTHOR);
		  mBook.mTranslator = response.isNull(Book.COLUMN_TRANSLATOR)?"":response.getString(Book.COLUMN_TRANSLATOR);
		  mBook.mPublisher = response.isNull(Book.COLUMN_PUBLISHER)?"":response.getString(Book.COLUMN_PUBLISHER);
		  mBook.mPubdate = response.isNull(Book.COLUMN_PUBDATE)?"":response.getString(Book.COLUMN_PUBDATE);
		  mBook.mRating = response.isNull(Book.COLUMN_RATING)?"":response.getString(Book.COLUMN_RATING);
		  mBook.mTags = response.isNull(Book.COLUMN_TAGS)?"":response.getString(Book.COLUMN_TAGS);
		  mBook.mBinding = response.isNull(Book.COLUMN_BINDING)?"":response.getString(Book.COLUMN_BINDING);
		  mBook.mPrice = response.isNull(Book.COLUMN_PRICE)?"":response.getString(Book.COLUMN_PRICE);
		  mBook.mSeries = response.isNull(Book.COLUMN_SERIES)?"":response.getString(Book.COLUMN_SERIES);
		  mBook.mPages = response.isNull(Book.COLUMN_PAGES)?"":response.getString(Book.COLUMN_PAGES);
		  mBook.mAuthorIntro = response.isNull(Book.COLUMN_AUTHOR_INTRO)?"":response.getString(Book.COLUMN_AUTHOR_INTRO);
		  mBook.mSummary = response.isNull(Book.COLUMN_SUMMARY)?"":response.getString(Book.COLUMN_SUMMARY);
		  mBook.mCatelog = response.isNull(Book.COLUMN_CATELOG)?"":response.getString(Book.COLUMN_CATELOG);
		  mBook.mEBookUrl = response.isNull(Book.COLUMN_EBOOK_URL)?"":response.getString(Book.COLUMN_EBOOK_URL);
		  mBook.mEBookPrice = response.isNull(Book.COLUMN_EBOOK_PRICE)?"":response.getString(Book.COLUMN_EBOOK_PRICE);
	  }catch(JSONException e){
	  }
	  return mBook;
  }
  public int saveBookToSQL(Book mBook, int state){
		ContentResolver mResolver = mActivity.getContentResolver();
		Cursor qCur = mResolver.query(Book.CONTENT_URI_BOOKS, null, Book.COLUMN_ISBN13 + "='" + mBook.mISBN13 + "'", null, null);
		if(qCur.getCount() > 0){
			Log.d("qiqi", "state:" + state + " mBook.mState:" + mBook.mState);
			if(state == 2 && mBook.mState == 1){
				ContentValues values = new ContentValues();
				values.put(Book.COLUMN_STATE, state);
				mResolver.update(Book.CONTENT_URI_BOOKS, values, Book.COLUMN_ISBN13 + "='" +mBook.mISBN13 + "'", null);
				return INSERT_ADD_SUCCESS;
			}else{
				return INSERT_ADDED;//������
			}
		}
		mBook.mAddTime = System.currentTimeMillis();
		  mBook.mState = state;
		ContentValues values = new ContentValues();
		values.put(Book.COLUMN_DOUBAN_ID, mBook.mDoubanId);
		values.put(Book.COLUMN_ISBN10, mBook.mISBN10);
		values.put(Book.COLUMN_ISBN13, mBook.mISBN13);
		values.put(Book.COLUMN_TITLE, mBook.mTitle);
		values.put(Book.COLUMN_ORIGIN_TITLE, mBook.mOriginTitle);
		values.put(Book.COLUMN_ALT_TITLE, mBook.mAltTitle);
		values.put(Book.COLUMN_SUB_TITLE, mBook.mSubTitle);
		values.put(Book.COLUMN_URL, mBook.mUrl);
		values.put(Book.COLUMN_ALT, mBook.mAlt);
		try {
			values.put(Book.COLUMN_IMAGE, (new JSONObject(mBook.mImages)).getString("large"));
		} catch (JSONException e) {
			values.put(Book.COLUMN_IMAGE, mBook.mImage);
		}
		values.put(Book.COLUMN_IMAGES, mBook.mImages);
		values.put(Book.COLUMN_AUTHOR, mBook.mAuthor);
		values.put(Book.COLUMN_TRANSLATOR, mBook.mTranslator);
		values.put(Book.COLUMN_PUBLISHER, mBook.mPublisher);
		values.put(Book.COLUMN_PUBDATE, mBook.mPubdate);
		values.put(Book.COLUMN_RATING, mBook.mRating);
		values.put(Book.COLUMN_TAGS, mBook.mTags);
		values.put(Book.COLUMN_BINDING, mBook.mBinding);
		values.put(Book.COLUMN_PRICE, mBook.mPrice);
		values.put(Book.COLUMN_SERIES, mBook.mSeries);
		values.put(Book.COLUMN_PAGES, mBook.mPages);
		values.put(Book.COLUMN_AUTHOR_INTRO,mBook. mAuthorIntro);
		values.put(Book.COLUMN_SUMMARY, mBook.mSummary);
		values.put(Book.COLUMN_CATELOG, mBook.mCatelog);
		values.put(Book.COLUMN_EBOOK_URL, mBook.mEBookUrl);
		values.put(Book.COLUMN_EBOOK_PRICE, mBook.mEBookPrice);
		values.put(Book.COLUMN_ADD_TIME, mBook.mAddTime);
		values.put(Book.COLUMN_STATE, mBook.mState);
		if(mResolver.insert(Book.CONTENT_URI_BOOKS, values) != null){
			if(mBook.mState == 1){
				return INSERT_WANT_SUCCESS;//success
			}else if (mBook.mState == 2){
				return INSERT_ADD_SUCCESS;//success
			}
		}
		if(state == 1){
			return INSERT_WANT_FAILED;
		}else{
			return INSERT_ADD_FAILED;
		}
  }
  public ISBNResultHandler(Activity activity, ParsedResult result, Result rawResult) {
    super(activity, result, rawResult);
    mActivity = activity;
  }

  @Override
  public int getButtonCount() {
    return hasCustomProductSearch() ? buttons.length : buttons.length - 1;
  }

  @Override
  public int getButtonText(int index) {
    return buttons[index];
  }

  @Override
  public void handleButtonPress(int index) {
    ISBNParsedResult isbnResult = (ISBNParsedResult) getResult();
    switch (index) {
      case 0:
        openProductSearch(isbnResult.getISBN());
        break;
      case 1:
        openBookSearch(isbnResult.getISBN());
        break;
      case 2:
        searchBookContents(isbnResult.getISBN());
        break;
      case 3:
        openURL(fillInCustomSearchURL(isbnResult.getISBN()));
        break;
    }
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_isbn;
  }
}
