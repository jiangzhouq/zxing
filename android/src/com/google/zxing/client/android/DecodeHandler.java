/*
 * Copyright (C) 2010 ZXing authors
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

import java.io.ByteArrayOutputStream;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dtr.zbar.build.ZBarDecoder;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final CaptureActivity activity;
  private final MultiFormatReader multiFormatReader;
  private boolean running = true;

  DecodeHandler(CaptureActivity activity, Map<DecodeHintType,Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    if (!running) {
      return;
    }
    switch (message.what) {
      case R.id.decode:
        decode((byte[]) message.obj, message.arg1, message.arg2);
        break;
      case R.id.quit:
        running = false;
        Looper.myLooper().quit();
        break;
    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
	// 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
      byte[] rotatedData = new byte[data.length];
      for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++)
              rotatedData[x * height + height - y - 1] = data[x + y * width];
      }
      
//	  byte[] rotatedData = new byte[data.length];
//	  for (int y = 0; y < height; y++) {
//	      for (int x = 0; x < width; x++)
//	          rotatedData[x * height + height - y - 1] = data[x + y * width];
//	  }
	  int tmp = width;
	  width = height;
	  height = tmp;

	  Rect rect = activity.getCameraManager().getFramingRectInPreview();

      ZBarDecoder zBarDecoder = new ZBarDecoder();
      String result = zBarDecoder.decodeCrop(rotatedData, width, height, rect.left, rect.top, rect.width(), rect.height());

      if (result != null) {
          if (null != activity.getHandler()) {
              Message msg = new Message();
              msg.obj = new Result(result, null, null, null);
              msg.what = R.id.decode_succeeded;
              activity.getHandler().sendMessage(msg);
              
          }
          // Message message = Message.obtain(activity.getHandler(),
          // R.id.decode_succeeded, result);
          // if (null != message) {
          // message.sendToTarget();
          // }
      } else {
          // Message message = Message.obtain(activity.getHandler(),
          // R.id.decode_failed);
          // if (null != message) {
          // message.sendToTarget();
          // }
          if (null != activity.getHandler()) {
              activity.getHandler().sendEmptyMessage(R.id.decode_failed);
          }
      }
      
//    long start = System.currentTimeMillis();
//    Result rawResult = null;
//    PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(rotatedData, width, height);
//    if (source != null) {
//      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//      try {
//        rawResult = multiFormatReader.decodeWithState(bitmap);
//      } catch (ReaderException re) {
//        // continue
//      } finally {
//        multiFormatReader.reset();
//      }
//    }
//
//    Handler handler = activity.getHandler();
//    if (rawResult != null) {
//      // Don't log the barcode contents for security.
//      long end = System.currentTimeMillis();
//      Log.d(TAG, "Found barcode in " + (end - start) + " ms");
//      if (handler != null) {
//        Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
//        Bundle bundle = new Bundle();
//        bundleThumbnail(source, bundle);        
//        message.setData(bundle);
//        message.sendToTarget();
//      }
//    } else {
//      if (handler != null) {
//        Message message = Message.obtain(handler, R.id.decode_failed);
//        message.sendToTarget();
//      }
//    }
  }

  private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
    int[] pixels = source.renderThumbnail();
    int width = source.getThumbnailWidth();
    int height = source.getThumbnailHeight();
    Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream out = new ByteArrayOutputStream();    
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
    bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
  }

}
