package com.google.zxing.client.android.history;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class BookProvider extends ContentProvider {
	public static final String DB_NAME = "bookstore.db";

    public static final String URI_AUTHORITY = "com.qjizho.bookstore";
    
    private static final int URI_CODE_BOOKS = 2;
    private static final int URI_CODE_BOOK = 1;
    public static final String URI_MIME_BOOKS
    = "vnd.android.cursor.dir/vnd.bookstore.book";
    public static final String URI_MIME_BOOK
        = "vnd.android.cursor.item/vnd.bookstore.book";

    private static final String TAG = BookProvider.class.getSimpleName();

    private static UriMatcher mUriMatcher;
    private BookDBHelper mBookDBHelper;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(URI_AUTHORITY, "book/#", URI_CODE_BOOK);
        mUriMatcher.addURI(URI_AUTHORITY, "books", URI_CODE_BOOKS);
    }

	@Override
	public boolean onCreate() {
		mBookDBHelper = new BookDBHelper(getContext(), DB_NAME, 1);
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		Log.e("memo", "=======================================uri:" + uri);
		switch (mUriMatcher.match(uri)) {
	        case URI_CODE_BOOK:
	            return URI_MIME_BOOK;
	        case URI_CODE_BOOKS:
	        	return URI_MIME_BOOKS;
	        default:
	            Log.e(TAG, "Unknown URI:" + uri);
	            throw new IllegalArgumentException("Unknown URI:" + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId;
		Uri rowUri = null;
		SQLiteDatabase db = mBookDBHelper.getWritableDatabase();
		switch (mUriMatcher.match(uri)) {
			case URI_CODE_BOOKS:
				rowId = db.insert(Book.TABLE_NAME, null, values);
				if (rowId != -1) {
					rowUri = ContentUris.withAppendedId(uri, rowId);
					return rowUri;
				}
				break;
		}
//		db.close();
		return null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		SQLiteDatabase db = mBookDBHelper.getWritableDatabase();
		switch (mUriMatcher.match(uri)) {
		case URI_CODE_BOOKS:
			count = db.delete(Book.TABLE_NAME, selection, selectionArgs);
			break;
		case URI_CODE_BOOK:
			long id = ContentUris.parseId(uri);
			String where = Book.COLUMN_ID + "=" + id;
			if(selection != null && !selection.equals("")){
				where = where + " and " + selection;
			}
			count = db.delete(Book.TABLE_NAME, where, selectionArgs);
			break;
		default:
			Log.e(TAG, "Unknown URI:" + uri);
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
//		db.close();
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mBookDBHelper.getReadableDatabase();
		int count;
		switch (mUriMatcher.match(uri)) {
		case URI_CODE_BOOKS:
			count = db.update(Book.TABLE_NAME, values, selection, selectionArgs);
			break;
		case URI_CODE_BOOK:
			long id = ContentUris.parseId(uri);
			String where = Book.COLUMN_ID + "=" + id;
			if(selection != null && !selection.equals("")){
				where = where + " and " + selection;
			}
			count = db.update(Book.TABLE_NAME, values, where, selectionArgs);
			break;
		default:
			Log.e(TAG, "Unknown URI:" + uri);
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
//		db.close();
		return count;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mBookDBHelper.getReadableDatabase();
		switch (mUriMatcher.match(uri)) {
			case URI_CODE_BOOKS:
				return db.query(Book.TABLE_NAME,projection,selection,selectionArgs,null,null,sortOrder);
			case URI_CODE_BOOK:
				long id = ContentUris.parseId(uri);
				String where = Book.COLUMN_ID + "=" + id;
				if(selection != null && !selection.equals("")){
					where = where + " and " + selection;
				}
				return db.query(Book.TABLE_NAME,projection,where,selectionArgs,null,null,sortOrder);
			default:
				Log.e(TAG, "Unknown URI:" + uri);
				throw new IllegalArgumentException("Unknown URI:" + uri);
		}
	}
	
}