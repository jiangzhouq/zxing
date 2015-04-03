package com.google.zxing.client.android.history;

import android.content.ContentValues;
import android.net.Uri;

public class Book {

	public final static String TABLE_NAME = "book";
	
	public final static String COLUMN_ID = "_id";
	public final static String COLUMN_DOUBAN_ID = "id";
	public final static String COLUMN_ISBN10 = "isbn10";
	public final static String COLUMN_ISBN13 = "isbn13";
	public final static String COLUMN_TITLE = "title";
	public final static String COLUMN_ORIGIN_TITLE = "origin_title";
	public final static String COLUMN_ALT_TITLE = "alt_title";
	public final static String COLUMN_SUB_TITLE = "subtitle";
	public final static String COLUMN_URL = "url";
	public final static String COLUMN_ALT = "alt";
	public final static String COLUMN_IMAGE = "image";
	public final static String COLUMN_IMAGES = "images";
	public final static String COLUMN_AUTHOR = "author";
	public final static String COLUMN_TRANSLATOR = "translator";
	public final static String COLUMN_PUBLISHER = "publisher";
	public final static String COLUMN_PUBDATE = "pubdate";
	public final static String COLUMN_RATING = "rating";
	public final static String COLUMN_TAGS = "tags";
	public final static String COLUMN_BINDING = "binding";
	public final static String COLUMN_PRICE = "price";
	public final static String COLUMN_SERIES = "series";
	public final static String COLUMN_PAGES = "pages";
	public final static String COLUMN_AUTHOR_INTRO = "author_intro";
	public final static String COLUMN_SUMMARY = "summary";
	public final static String COLUMN_CATELOG = "catalog";
	public final static String COLUMN_EBOOK_URL = "ebook_url";
	public final static String COLUMN_EBOOK_PRICE = "ebook_price";
	public final static String COLUMN_ADD_TIME = "add_time";
	public final static String COLUMN_STATE = "state";
	
	public final static int NUM_COLUMN_ID = 0;
	public final static int NUM_COLUMN_DOUBAN_ID = 1;
	public final static int NUM_COLUMN_ISBN10 = 2;
	public final static int NUM_COLUMN_ISBN13 = 3;
	public final static int NUM_COLUMN_TITLE = 4;
	public final static int NUM_COLUMN_ORIGIN_TITLE = 5;
	public final static int NUM_COLUMN_ALT_TITLE = 6;
	public final static int NUM_COLUMN_SUB_TITLE = 7;
	public final static int NUM_COLUMN_URL = 8;
	public final static int NUM_COLUMN_ALT = 9;
	public final static int NUM_COLUMN_IMAGE = 10;
	public final static int NUM_COLUMN_IMAGES = 11;
	public final static int NUM_COLUMN_AUTHOR = 12;
	public final static int NUM_COLUMN_TRANSLATOR = 13;
	public final static int NUM_COLUMN_PUBLISHER = 14;
	public final static int NUM_COLUMN_PUBDATE = 15;
	public final static int NUM_COLUMN_RATING = 16;
	public final static int NUM_COLUMN_TAGS = 17;
	public final static int NUM_COLUMN_BINDING = 18;
	public final static int NUM_COLUMN_PRICE = 19;
	public final static int NUM_COLUMN_SERIES = 20;
	public final static int NUM_COLUMN_PAGES = 21;
	public final static int NUM_COLUMN_AUTHOR_INTRO = 22;
	public final static int NUM_COLUMN_SUMMARY = 23;
	public final static int NUM_COLUMN_CATELOG = 24;
	public final static int NUM_COLUMN_EBOOK_URL = 25;
	public final static int NUM_COLUMN_EBOOK_PRICE = 26;
	public final static int NUM_COLUMN_ADD_TIME = 27;
	public final static int NUM_COLUMN_STATE = 28;
	
	public final static int BOOK_STATE_NO = 0;
	public final static int BOOK_STATE_WANT = 1;
	public final static int BOOK_STATE_HAS = 2;
	public final static int BOOK_STATE_READED = 3;
	
	public final static Uri CONTENT_URI_BOOKS = Uri.parse("content://"
			+ BookProvider.URI_AUTHORITY + "/books");
	public final static Uri CONTENT_URI_BOOK = Uri.parse("content://"
			+ BookProvider.URI_AUTHORITY + "/book");
	public long mId;
	public String mDoubanId;
	public String mISBN10;
	public String mISBN13;
	public String mTitle;
	public String mOriginTitle;
	public String mAltTitle;
	public String mSubTitle;
	public String mUrl;
	public String mAlt;
	public String mImage;
	public String mImages;
	public String mAuthor;
	public String mTranslator;
	public String mPublisher;
	public String mPubdate;
	public String mRating;
	public String mTags;
	public String mBinding;
	public String mPrice;
	public String mSeries;
	public String mPages;
	public String mAuthorIntro;
	public String mSummary;
	public String mCatelog;
	public String mEBookUrl;
	public String mEBookPrice;
	public Long mAddTime;
	public int mState;
	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if(mId != 0) {
			values.put(COLUMN_ID, mId);
		}
		values.put(COLUMN_DOUBAN_ID, mDoubanId);
		values.put(COLUMN_ISBN10, mISBN10);
		values.put(COLUMN_ISBN13, mISBN13);
		values.put(COLUMN_TITLE, mTitle);
		values.put(COLUMN_ORIGIN_TITLE, mOriginTitle);
		values.put(COLUMN_ALT_TITLE, mAltTitle);
		values.put(COLUMN_SUB_TITLE, mSubTitle);
		values.put(COLUMN_URL, mUrl);
		values.put(COLUMN_ALT, mAlt);
		values.put(COLUMN_IMAGE, mImage);
		values.put(COLUMN_IMAGES, mImages);
		values.put(COLUMN_AUTHOR, mAuthor);
		values.put(COLUMN_TRANSLATOR, mTranslator);
		values.put(COLUMN_PUBLISHER, mPublisher);
		values.put(COLUMN_PUBDATE, mPubdate);
		values.put(COLUMN_RATING, mRating);
		values.put(COLUMN_TAGS, mTags);
		values.put(COLUMN_BINDING, mBinding);
		values.put(COLUMN_PRICE, mPrice);
		values.put(COLUMN_SERIES, mSeries);
		values.put(COLUMN_PAGES, mPages);
		values.put(COLUMN_AUTHOR_INTRO, mAuthorIntro);
		values.put(COLUMN_SUMMARY, mSummary);
		values.put(COLUMN_CATELOG, mCatelog);
		values.put(COLUMN_EBOOK_URL, mEBookUrl);
		values.put(COLUMN_EBOOK_PRICE, mEBookPrice);
		values.put(COLUMN_ADD_TIME, mAddTime);
		values.put(COLUMN_STATE, mState);
		return values;
	}
}