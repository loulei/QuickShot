package com.example.quickshot;

import android.app.Application;
import android.graphics.Bitmap;

/**
 * Created by loulei on 17-12-18.
 */

public class MainApp extends Application {

    public Bitmap getScreenCaptureBitmap() {
        return screenCaptureBitmap;
    }

    public void setScreenCaptureBitmap(Bitmap screenCaptureBitmap) {
        this.screenCaptureBitmap = screenCaptureBitmap;
    }

    private Bitmap screenCaptureBitmap;

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
