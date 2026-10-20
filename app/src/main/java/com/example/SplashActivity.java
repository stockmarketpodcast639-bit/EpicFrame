package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity presents an elegant branded introduction screen for Epic Frame
 * before quickly transitioning to MainActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 800;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout centerLayout = findViewById(R.id.layoutCenter);
        if (centerLayout != null) {
            Animation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(500);
            fadeIn.setFillAfter(true);
            centerLayout.startAnimation(fadeIn);
        }

        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMain();
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToMain();
            }
        }, SPLASH_DURATION_MS);
    }

    private synchronized void navigateToMain() {
        if (isNavigating || isFinishing() || isDestroyed()) return;
        isNavigating = true;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
