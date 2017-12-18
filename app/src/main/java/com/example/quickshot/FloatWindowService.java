package com.example.quickshot;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by loulei on 17-12-18.
 */
@android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FloatWindowService extends Service {

    public static Intent newIntent(Context context, Intent resultData) {
        Intent intent = new Intent(context, FloatWindowService.class);
        if (resultData != null) {
            intent.putExtras(resultData);
        }
        return intent;
    }

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private static Intent resultData;

    public static Intent getResultData() {
        return resultData;
    }

    public static void setResultData(Intent resultData) {
        FloatWindowService.resultData = resultData;
    }

    private ImageReader imageReader;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private GestureDetector gestureDetector;

    private ImageView floatView;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatView();
        createImageReader();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createImageReader() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
    }

    private void createFloatView() {
        gestureDetector = new GestureDetector(getApplicationContext(), new FloatGestureTouchListener());
        layoutParams = new WindowManager.LayoutParams();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        layoutParams.format = PixelFormat.RGBA_8888;

        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.x = screenWidth;
        layoutParams.y = 100;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        floatView = new ImageView(getApplicationContext());
        floatView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        windowManager.addView(floatView, layoutParams);

        floatView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void startScreenShot() {
        floatView.setVisibility(View.GONE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startVirtual();
            }
        }, 5);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startCapture();
            }
        }, 30);
    }

    private void startCapture() {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            startScreenShot();
        } else {
            SaveTask saveTask = new SaveTask();
            AsyncTaskCompat.executeParallel(saveTask, image);
        }
    }


    public void startVirtual() {
        if (mediaProjection != null) {
            virtualDisplay();
        } else {
            setupMediaProjection();
            virtualDisplay();
        }
    }

    public void setupMediaProjection() {
        if (resultData == null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        } else {
            mediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, resultData);
        }
    }

    private MediaProjectionManager getMediaProjectionManager() {
        return (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }


    private void virtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("screen-mirror", screenWidth, screenHeight, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
    }

    private class FloatGestureTouchListener implements GestureDetector.OnGestureListener {

        int lastX, lastY;
        int paramX, paramY;

        @Override
        public boolean onDown(MotionEvent e) {
            lastX = (int) e.getRawX();
            lastY = (int) e.getRawY();
            paramX = layoutParams.x;
            paramY = layoutParams.y;
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            startScreenShot();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int dx = (int) (e2.getRawX() - lastX);
            int dy = (int) (e2.getRawY() - lastY);
            layoutParams.x = paramX + dx;
            layoutParams.y = paramY + dy;
            windowManager.updateViewLayout(floatView, layoutParams);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Image... params) {
            if (params == null || params.length < 1 || params[0] == null) {
                return null;
            }
            Image image = params[0];
            int width = image.getWidth();
            int height = image.getHeight();
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();
            File fileImage = null;
            if (bitmap != null) {
                try {
                    fileImage = new File(getScreenShotsName(getApplicationContext()));
                    if (!fileImage.exists()) {
                        fileImage.createNewFile();
                    }
                    FileOutputStream out = new FileOutputStream(fileImage);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        out.close();
                        Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(fileImage);
                        media.setData(contentUri);
                        sendBroadcast(media);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (fileImage != null) {
                    return bitmap;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                ((MainApp) getApplication()).setScreenCaptureBitmap(bitmap);
                startActivity(PreviewActivity.newIntent(getApplicationContext()));
            }
            floatView.setVisibility(View.VISIBLE);
        }
    }



    public static final String SCREENCAPTURE_PATH = "ScreenCapture" + File.separator + "Screenshots" +File.separator;
    public static final String SCREENSHOT_NAME = "Screenshot";

    public static String getAppPath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory().toString();
        } else {
            return context.getFilesDir().toString();
        }
    }

    public static String getScreenShots(Context context) {
        StringBuffer stringBuffer = new StringBuffer(getAppPath(context));
        stringBuffer.append(File.separator);
        stringBuffer.append(SCREENCAPTURE_PATH);
        File file = new File(stringBuffer.toString());
        if (!file.exists()) {
            file.mkdirs();
        }
        return stringBuffer.toString();
    }

    public static String getScreenShotsName(Context context) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = simpleDateFormat.format(new Date());
        StringBuffer stringBuffer = new StringBuffer(getScreenShots(context));
        stringBuffer.append(SCREENSHOT_NAME);
        stringBuffer.append("_");
        stringBuffer.append(date);
        stringBuffer.append(".png");
        return stringBuffer.toString();
    }
}
