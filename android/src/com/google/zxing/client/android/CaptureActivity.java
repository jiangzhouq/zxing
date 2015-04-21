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

package com.google.zxing.client.android;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.clipboard.ClipboardInterface;
import com.google.zxing.client.android.history.Book;
import com.google.zxing.client.android.history.HistoryActivity;
import com.google.zxing.client.android.history.HistoryItem;
import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.android.result.ISBNResultHandler;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.android.share.ShareActivity;
import com.leaking.slideswitch.SlideSwitch;
import com.leaking.slideswitch.SlideSwitch.SlideListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback ,OnClickListener {

  private static final String TAG = CaptureActivity.class.getSimpleName();

  private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;


  public static final int HISTORY_REQUEST_CODE = 0x0000bacc;


  private CameraManager cameraManager;
  private CaptureActivityHandler handler;
  private Result savedResultToShow;
  private ViewfinderView viewfinderView;
  private TextView statusView;
  private View resultView;
  private Result lastResult;
  private boolean hasSurface;
  private boolean copyToClipboard;
  private Collection<BarcodeFormat> decodeFormats;
  private Map<DecodeHintType,?> decodeHints;
  private String characterSet;
  private HistoryManager historyManager;
  private InactivityTimer inactivityTimer;
  private BeepManager beepManager;
  private AmbientLightManager ambientLightManager;

  private SlideSwitch mSlide;
  private boolean mContiScan = false;
  ViewfinderView getViewfinderView() {
    return viewfinderView;
  }

  public Handler getHandler() {
    return handler;
  }

  CameraManager getCameraManager() {
    return cameraManager;
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    hasSurface = false;
    inactivityTimer = new InactivityTimer(this);
    beepManager = new BeepManager(this);
    ambientLightManager = new AmbientLightManager(this);

    mSlide = (SlideSwitch) findViewById(R.id.slideswitch);
    mSlide.setSlideListener(new SlideListener() {
		
		@Override
		public void open() {
			mContiScan = true;
		}
		
		@Override
		public void close() {
			mContiScan = false;
		}
	});
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    // historyManager must be initialized here to update the history preference
    historyManager = new HistoryManager(this);
    historyManager.trimHistory();

    // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
    // want to open the camera driver and measure the screen size if we're going to show the help on
    // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
    // off screen.
    cameraManager = new CameraManager(getApplication());

    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    viewfinderView.setCameraManager(cameraManager);

    resultView = findViewById(R.id.result_view);
    statusView = (TextView) findViewById(R.id.status_view);

    handler = null;
    lastResult = null;

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

//    if (prefs.getBoolean(PreferencesActivity.KEY_DISABLE_AUTO_ORIENTATION, true)) {
//      setRequestedOrientation(getCurrentOrientation());
//    } else {
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//    }

    resetStatusView();


    beepManager.updatePrefs();
    ambientLightManager.start(cameraManager);

    inactivityTimer.onResume();

    decodeFormats = null;
    characterSet = null;


    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
    }
  }

//  private int getCurrentOrientation() {
//    int rotation = getWindowManager().getDefaultDisplay().getRotation();
//    switch (rotation) {
//      case Surface.ROTATION_0:
//      case Surface.ROTATION_90:
//        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//      default:
//        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
//    }
//  }
  

  @Override
  protected void onPause() {
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    inactivityTimer.onPause();
    ambientLightManager.stop();
    beepManager.close();
    cameraManager.closeDriver();
    //historyManager = null; // Keep for onActivityResult
    if (!hasSurface) {
      SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(this);
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    inactivityTimer.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_BACK:
    	  if(null != lastResult){
    		  restartPreviewAfterDelay(0L);
    	  }else{
    		  finish();
    	  }
	      return true;
      case KeyEvent.KEYCODE_FOCUS:
      case KeyEvent.KEYCODE_CAMERA:
        // Handle these events so they don't launch the Camera app
        return true;
      // Use volume up/down to turn on light
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        cameraManager.setTorch(false);
        return true;
      case KeyEvent.KEYCODE_VOLUME_UP:
        cameraManager.setTorch(true);
        return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
//    MenuInflater menuInflater = getMenuInflater();
//    menuInflater.inflate(R.menu.capture, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    switch (item.getItemId()) {
      case R.id.menu_share:
        intent.setClassName(this, ShareActivity.class.getName());
        startActivity(intent);
        break;
      case R.id.menu_history:
        intent.setClassName(this, HistoryActivity.class.getName());
        startActivityForResult(intent, HISTORY_REQUEST_CODE);
        break;
      case R.id.menu_settings:
        intent.setClassName(this, PreferencesActivity.class.getName());
        startActivity(intent);
        break;
      case R.id.menu_help:
        intent.setClassName(this, HelpActivity.class.getName());
        startActivity(intent);
        break;
      default:
        return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (resultCode == RESULT_OK) {
      if (requestCode == HISTORY_REQUEST_CODE) {
        int itemNumber = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1);
        if (itemNumber >= 0) {
          HistoryItem historyItem = historyManager.buildHistoryItem(itemNumber);
          decodeOrStoreSavedBitmap(null, historyItem.getResult());
        }
      }
    }
  }

  private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
    // Bitmap isn't used yet -- will be used soon
    if (handler == null) {
      savedResultToShow = result;
    } else {
      if (result != null) {
        savedResultToShow = result;
      }
      if (savedResultToShow != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
        handler.sendMessage(message);
      }
      savedResultToShow = null;
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    if (holder == null) {
      Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
    }
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show the results.
   *
   * @param rawResult The contents of the barcode.
   * @param scaleFactor amount by which thumbnail was scaled
   * @param barcode   A greyscale bitmap of the camera data which was decoded.
   */
  public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
    inactivityTimer.onActivity();
    lastResult = rawResult;
    ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);

    boolean fromLiveScan = barcode != null;
    if (fromLiveScan) {
      historyManager.addHistoryItem(rawResult, resultHandler);
      // Then not from history, so beep/vibrate and we have an image to draw on
      drawResultPoints(barcode, scaleFactor, rawResult);
    }
    beepManager.playBeepSoundAndVibrate();
    
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    if (fromLiveScan && prefs.getBoolean(PreferencesActivity.KEY_BULK_MODE, false)) {
      Toast.makeText(getApplicationContext(),
                     getResources().getString(R.string.msg_bulk_mode_scanned) + " (" + rawResult.getText() + ')',
                     Toast.LENGTH_SHORT).show();
      // Wait a moment or else it will scan the same barcode continuously about 3 times
      restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
    } else {
      handleDecodeInternally(rawResult, resultHandler, barcode);
    }
  }

  /**
   * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
   *
   * @param barcode   A bitmap of the captured image.
   * @param scaleFactor amount by which thumbnail was scaled
   * @param rawResult The decoded results which contains the points to draw.
   */
  private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      Canvas canvas = new Canvas(barcode);
      Paint paint = new Paint();
      paint.setColor(getResources().getColor(R.color.result_points));
      if (points.length == 2) {
        paint.setStrokeWidth(4.0f);
        drawLine(canvas, paint, points[0], points[1], scaleFactor);
      } else if (points.length == 4 &&
                 (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                  rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
        // Hacky special case -- draw two lines, for the barcode and metadata
        drawLine(canvas, paint, points[0], points[1], scaleFactor);
        drawLine(canvas, paint, points[2], points[3], scaleFactor);
      } else {
        paint.setStrokeWidth(10.0f);
        for (ResultPoint point : points) {
          if (point != null) {
            canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
          }
        }
      }
    }
  }

  private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
    if (a != null && b != null) {
      canvas.drawLine(scaleFactor * a.getX(), 
                      scaleFactor * a.getY(), 
                      scaleFactor * b.getX(), 
                      scaleFactor * b.getY(), 
                      paint);
    }
  }
  private StringBuilder jsonArrayToString(JSONArray array){
	  StringBuilder strB = new StringBuilder();
	  for (int i = 0 ; i < array.length(); i++){
		 try {
			strB.append(array.getString(i));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	  }
	  return strB;
  }
  
	private void resetResult() {
		TextView contentsTitle = (TextView) findViewById(R.id.meta_result_title);
		TextView contentsOriginTitle = (TextView) findViewById(R.id.meta_result_origin_title);
		TextView contentsAuthor = (TextView) findViewById(R.id.meta_result_author);
		TextView contentsTranslator = (TextView) findViewById(R.id.meta_result_translator);
		TextView contentsPublisher = (TextView) findViewById(R.id.meta_result_publisher);
		TextView contentsPubdate = (TextView) findViewById(R.id.meta_result_pubdate);
		TextView contentsRating = (TextView) findViewById(R.id.meta_result_rating);
		ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
		contentsTitle.setText("...");
		contentsOriginTitle.setText("...");
		contentsAuthor.setText("...");
		contentsTranslator.setText("...");
		contentsPublisher.setText("...");
		contentsPubdate.setText("...");
		contentsRating.setText("...");
		barcodeImageView.setImageURI(null);
		Button btnAdd = (Button) findViewById(R.id.add);
	  	Button btnWant = (Button) findViewById(R.id.want);
	  	btnAdd.setBackgroundResource(R.drawable.result_btn_bg);
	  	btnWant.setBackgroundResource(R.drawable.result_btn_bg);
	}
  
	private void showToast(String name,int i){
		switch(i){
		case ISBNResultHandler.INSERT_ADD_SUCCESS:
			Toast.makeText(this, String.format(getResources().getString(R.string.book_insert_add_success), name), Toast.LENGTH_SHORT).show();
			break;
		case ISBNResultHandler.INSERT_ADD_FAILED:
			Toast.makeText(this, String.format(getResources().getString(R.string.book_insert_add_failed), name), Toast.LENGTH_SHORT).show();
			break;
		case ISBNResultHandler.INSERT_WANT_SUCCESS:
			Toast.makeText(this, String.format(getResources().getString(R.string.book_insert_want_success), name), Toast.LENGTH_SHORT).show();
			break;
		case ISBNResultHandler.INSERT_WANT_FAILED:
			Toast.makeText(this, String.format(getResources().getString(R.string.book_insert_want_failed), name), Toast.LENGTH_SHORT).show();
			break;
		case ISBNResultHandler.INSERT_ADDED:
			Toast.makeText(this, String.format(getResources().getString(R.string.book_insert_added), name), Toast.LENGTH_SHORT).show();
			break;
		case ISBNResultHandler.INSERT_WANTED:
			Toast.makeText(this, String.format(getResources().getString(R.string.book_insert_wanted), name), Toast.LENGTH_SHORT).show();
			break;
		}
	}
  public void doubanComplete(Book book, ISBNResultHandler handler){
	  Cursor c = getContentResolver().query(Book.CONTENT_URI_BOOKS, null, Book.COLUMN_ISBN13 +"='"+book.mISBN13 +"'", null, null);
	  int stateInSQL = 0;
	  Log.d("qiqi", "c count:" + c.getCount());
	  if(c.getCount() > 0){
		  c.moveToFirst();
		  stateInSQL = c.getInt(Book.NUM_COLUMN_STATE);
		  book.mState = stateInSQL;
		  Log.d("qiqi", "stateInSQL:" + stateInSQL + " book.mState:" + book.mState);
	  }
	  Log.d("qiqi", "stateInSQL:" + stateInSQL);
	  	TextView contentsTitle = (TextView) findViewById(R.id.meta_result_title);
	  	TextView contentsOriginTitle = (TextView) findViewById(R.id.meta_result_origin_title);
	  	TextView labelOriginTitle = (TextView) findViewById(R.id.result_page_origin_title);
	  	TextView contentsAuthor = (TextView) findViewById(R.id.meta_result_author);
	  	TextView contentsTranslator = (TextView) findViewById(R.id.meta_result_translator);
	  	TextView labelTranslator = (TextView) findViewById(R.id.result_page_translator);
	  	TextView contentsPublisher = (TextView) findViewById(R.id.meta_result_publisher);
	  	TextView contentsPubdate = (TextView) findViewById(R.id.meta_result_pubdate);
	  	TextView contentsRating = (TextView) findViewById(R.id.meta_result_rating);
	  	
	  	contentsTitle.setText(book.mTitle);
	  	if(book.mOriginTitle.isEmpty() || book.mOriginTitle.length() == 0){
	  		contentsOriginTitle.setVisibility(View.GONE);
	  		labelOriginTitle.setVisibility(View.GONE);
	  	}else{
	  		contentsOriginTitle.setText(book.mOriginTitle);
	  	}
	  	try {
			JSONArray jAuthor = new JSONArray(book.mAuthor);
			contentsAuthor.setText(jsonArrayToString(jAuthor).toString());
		} catch (JSONException e1) {
			e1.printStackTrace();
			contentsAuthor.setText(book.mAuthor);
		}
	  	if(book.mTranslator.isEmpty() || book.mTranslator.length() == 0){
	  		contentsTranslator.setVisibility(View.GONE);
	  		labelTranslator.setVisibility(View.GONE);
	  	}else{
	  		try {
	  			Log.d("qiqi", "count:" + book.mTranslator.length());
	  			JSONArray jTranslator = new JSONArray(book.mTranslator);
	  			if(jsonArrayToString(jTranslator).toString().length() == 0){
	  				contentsTranslator.setVisibility(View.GONE);
		  			labelTranslator.setVisibility(View.GONE);
	  			}else{
	  				contentsTranslator.setText(jsonArrayToString(jTranslator).toString());
	  			}
	  		} catch (Exception e) {
	  			contentsTranslator.setVisibility(View.GONE);
	  			labelTranslator.setVisibility(View.GONE);
	  		}
	  	}
	  	contentsPublisher.setText(book.mPublisher);
	  	contentsPubdate.setText(book.mPubdate);
	  	try {
			contentsRating.setText((new JSONObject(book.mRating)).getString("average"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			contentsRating.setText("");
		}
	  	ImageLoader imageLoader = ImageLoader.getInstance();
	  	imageLoader.init(ImageLoaderConfiguration.createDefault(this));
	  	ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
	  	try {
			imageLoader.displayImage((new JSONObject(book.mImages)).getString("large"), barcodeImageView);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  	final ISBNResultHandler isbnHandler = handler;
	  	final Book mBook = book;
	  	if(mContiScan){
	  		if(stateInSQL == 0){
  				showToast(mBook.mTitle, isbnHandler.saveBookToSQL(mBook, Book.BOOK_STATE_HAS));
		  	}else if(stateInSQL == 1){
  				showToast(mBook.mTitle, isbnHandler.saveBookToSQL(mBook, Book.BOOK_STATE_HAS));
		  	}else if(stateInSQL >=2){
  				showToast(mBook.mTitle,ISBNResultHandler.INSERT_ADDED);
		  	}
	  		restartPreviewAfterDelay(3000L);
	  	}
	  	Button btnAdd = (Button) findViewById(R.id.add);
	  	Button btnWant = (Button) findViewById(R.id.want);
	  	if(stateInSQL == 0){
	  		btnAdd.setOnClickListener(new OnClickListener() {
	  			
	  			@Override
	  			public void onClick(View v) {
	  				showToast(mBook.mTitle, isbnHandler.saveBookToSQL(mBook, Book.BOOK_STATE_HAS));
	  				restartPreviewAfterDelay(0L);
	  			}
	  		});
		  	btnWant.setOnClickListener(new OnClickListener() {
	  			@Override
	  			public void onClick(View v) {
	  				showToast(mBook.mTitle,isbnHandler.saveBookToSQL(mBook, Book.BOOK_STATE_WANT));
	  				restartPreviewAfterDelay(0L);
	  			}
	  		});
	  	}else if(stateInSQL == 1){
	  		btnWant.setBackgroundResource(R.drawable.result_btn_bg_unactive);
	  		btnAdd.setOnClickListener(new OnClickListener() {
	  			
	  			@Override
	  			public void onClick(View v) {
	  				showToast(mBook.mTitle, isbnHandler.saveBookToSQL(mBook, Book.BOOK_STATE_HAS));
	  				restartPreviewAfterDelay(0L);
	  			}
	  		});
		  	btnWant.setOnClickListener(new OnClickListener() {
	  			@Override
	  			public void onClick(View v) {
	  				showToast(mBook.mTitle,ISBNResultHandler.INSERT_WANTED);
	  			}
	  		});
	  	}else if(stateInSQL >=2){
	  		btnAdd.setBackgroundResource(R.drawable.result_btn_bg_unactive);
	  		btnWant.setBackgroundResource(R.drawable.result_btn_bg_unactive);
	  		btnAdd.setOnClickListener(new OnClickListener() {
	  			
	  			@Override
	  			public void onClick(View v) {
	  				showToast(mBook.mTitle,ISBNResultHandler.INSERT_ADDED);
	  			}
	  		});
		  	btnWant.setOnClickListener(new OnClickListener() {
	  			@Override
	  			public void onClick(View v) {
	  				showToast(mBook.mTitle,ISBNResultHandler.INSERT_ADDED);
	  			}
	  		});
	  	}
//	    int scaledSize = Math.max(22, 32 - book.mTitle.length() / 4);
//	    contentsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//	    contentsOriginTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//	    contentsAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//	    contentsTranslator.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//	    contentsPublisher.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//	    contentsPubdate.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//	    contentsRating.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
  }
  // Put up our own UI for how to handle the decoded contents.
  private void handleDecodeInternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {

    CharSequence displayContents = resultHandler.getDisplayContents();
    resultHandler.handleAuto(displayContents.toString());
    
    if (copyToClipboard && !resultHandler.areContentsSecure()) {
      ClipboardInterface.setText(displayContents, this);
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    if (resultHandler.getDefaultButtonID() != null && prefs.getBoolean(PreferencesActivity.KEY_AUTO_OPEN_WEB, false)) {
      resultHandler.handleButtonPress(resultHandler.getDefaultButtonID());
      return;
    }

    if(!mContiScan){
    	statusView.setVisibility(View.GONE);
    	viewfinderView.setVisibility(View.GONE);
    	resultView.setVisibility(View.VISIBLE);
    }

//    ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
//    if (barcode == null) {
//      barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
//          R.drawable.launcher_icon));
//    } else {
//      barcodeImageView.setImageBitmap(barcode);
//    }

//    TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
//    formatTextView.setText(rawResult.getBarcodeFormat().toString());
//
//    TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
//    typeTextView.setText(resultHandler.getType().toString());
//
//    DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
//    TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
//    timeTextView.setText(formatter.format(new Date(rawResult.getTimestamp())));
//
//
//    TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
//    View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
//    metaTextView.setVisibility(View.GONE);
//    metaTextViewLabel.setVisibility(View.GONE);
//    Map<ResultMetadataType,Object> metadata = rawResult.getResultMetadata();
//    if (metadata != null) {
//      StringBuilder metadataText = new StringBuilder(20);
//      for (Map.Entry<ResultMetadataType,Object> entry : metadata.entrySet()) {
//        if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
//          metadataText.append(entry.getValue()).append('\n');
//        }
//      }
//      if (metadataText.length() > 0) {
//        metadataText.setLength(metadataText.length() - 1);
//        metaTextView.setText(metadataText);
//        Log.d("qiqi", "metaTextView:"+metadataText);
//        metaTextView.setVisibility(View.VISIBLE);
//        metaTextViewLabel.setVisibility(View.VISIBLE);
//      }
//    }

//    TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
//    contentsTextView.setText(displayContents);
//    int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
//    contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
    
//    TextView supplementTextView = (TextView) findViewById(R.id.contents_supplement_text_view);
//    supplementTextView.setText("");
//    supplementTextView.setOnClickListener(null);
//    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
//        PreferencesActivity.KEY_SUPPLEMENTAL, true)) {
//      SupplementalInfoRetriever.maybeInvokeRetrieval(supplementTextView,
//                                                     resultHandler.getResult(),
//                                                     historyManager,
//                                                     this);
//    }
    
//    int buttonCount = resultHandler.getButtonCount();
//    ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
//    buttonView.requestFocus();
//    for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
//      TextView button = (TextView) buttonView.getChildAt(x);
//      if (x < buttonCount) {
//        button.setVisibility(View.VISIBLE);
//        button.setText(resultHandler.getButtonText(x));
//        button.setOnClickListener(new ResultButtonListener(resultHandler, x));
//      } else {
//        button.setVisibility(View.GONE);
//      }
//    }

  }

  // Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
  
  private void sendReplyMessage(int id, Object arg, long delayMS) {
    if (handler != null) {
      Message message = Message.obtain(handler, id, arg);
      if (delayMS > 0L) {
        handler.sendMessageDelayed(message, delayMS);
      } else {
        handler.sendMessage(message);
      }
    }
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }
    if (cameraManager.isOpen()) {
      Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
      return;
    }
    try {
      cameraManager.openDriver(surfaceHolder);
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      if (handler == null) {
        handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
      }
      decodeOrStoreSavedBitmap(null, null);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializing camera", e);
      displayFrameworkBugMessageAndExit();
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }

  public void restartPreviewAfterDelay(long delayMS) {
    if (handler != null) {
      handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
    }
    resetStatusView();
  }

  private void resetStatusView() {
    resultView.setVisibility(View.GONE);
    statusView.setText(R.string.msg_default_status);
    statusView.setVisibility(View.VISIBLE);
    viewfinderView.setVisibility(View.VISIBLE);
    lastResult = null;
    resetResult();
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }

@Override
public void onClick(View v) {
	switch(v.getId()){
	case R.id.want:
		break;
	case R.id.add:
		break;
	}
}
}
