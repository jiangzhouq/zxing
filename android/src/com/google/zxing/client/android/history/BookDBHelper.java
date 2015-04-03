/*
 * Copyright (C) 2009 ZXing authors
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

package com.google.zxing.client.android.history;

import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;

/**
 * @author Sean Owen
 */
final class BookDBHelper extends SQLiteOpenHelper {

	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "book.db";
	static final String TABLE_NAME = "history";
	static final String ID_COL = "id";
	static final String TEXT_COL = "text";
	static final String FORMAT_COL = "format";
	static final String DISPLAY_COL = "display";
	static final String TIMESTAMP_COL = "timestamp";
	static final String DETAILS_COL = "details";

	public BookDBHelper(Context context) {
		this(context, DB_NAME, null, DB_VERSION);
	}
	public BookDBHelper(Context context, String name, int version) {
		this(context, name, null, version);
	}

	public BookDBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

//	BookDBHelper(Context context) {
//		super(context, DB_NAME, null, DB_VERSION);
//	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + " (" + ID_COL
				+ " INTEGER PRIMARY KEY, " + TEXT_COL + " TEXT, " + FORMAT_COL
				+ " TEXT, " + DISPLAY_COL + " TEXT, " + TIMESTAMP_COL
				+ " INTEGER, " + DETAILS_COL + " TEXT);");
		sqLiteDatabase.execSQL("CREATE TABLE " + Book.TABLE_NAME + " ("
				+ Book.COLUMN_ID + " INTEGER PRIMARY KEY, "
				+ Book.COLUMN_DOUBAN_ID + " TEXT, " + Book.COLUMN_ISBN10
				+ " TEXT, " + Book.COLUMN_ISBN13 + " TEXT, "
				+ Book.COLUMN_TITLE + " TEXT, " + Book.COLUMN_ORIGIN_TITLE
				+ " TEXT, " + Book.COLUMN_ALT_TITLE + " TEXT, "
				+ Book.COLUMN_SUB_TITLE + " TEXT, " + Book.COLUMN_URL
				+ " TEXT, " + Book.COLUMN_ALT + " TEXT, " + Book.COLUMN_IMAGE
				+ " TEXT, " + Book.COLUMN_IMAGES + " TEXT, "
				+ Book.COLUMN_AUTHOR + " TEXT, " + Book.COLUMN_TRANSLATOR
				+ " TEXT, " + Book.COLUMN_PUBLISHER + " TEXT, "
				+ Book.COLUMN_PUBDATE + " TEXT, " + Book.COLUMN_RATING
				+ " TEXT, " + Book.COLUMN_TAGS + " TEXT, "
				+ Book.COLUMN_BINDING + " TEXT, " + Book.COLUMN_PRICE
				+ " TEXT, " + Book.COLUMN_SERIES + " TEXT, "
				+ Book.COLUMN_PAGES + " TEXT, " + Book.COLUMN_AUTHOR_INTRO
				+ " TEXT, " + Book.COLUMN_SUMMARY + " TEXT, "
				+ Book.COLUMN_CATELOG + " TEXT, " + Book.COLUMN_EBOOK_URL
				+ " TEXT, " + Book.COLUMN_EBOOK_PRICE + " TEXT, " 
				+Book.COLUMN_ADD_TIME + " LONG, " + Book.COLUMN_STATE
				+ " INTEGER);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion,
			int newVersion) {
		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(sqLiteDatabase);
	}

}
