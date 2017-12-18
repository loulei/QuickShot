package com.example.quickshot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

/**
 * Created by loulei on 17-12-18.
 */

public class PreviewActivity extends AppCompatActivity {

    public static final Intent newIntent(Context context) {
        Intent intent = new Intent(context, PreviewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    private ImageView iv_preview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        iv_preview = (ImageView) findViewById(R.id.iv_preview);

        Bitmap bitmap = ((MainApp) getApplication()).getScreenCaptureBitmap();
        iv_preview.setImageBitmap(bitmap);
    }
}
