package com.saiyanstudio.groceryassistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class SplashScreenActivity extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer
             */

            @Override
            public void run() {
                // Launch LoginOrSignup activity once the timer is over
                Intent intent = new Intent(SplashScreenActivity.this, LoginOrSignupActivity.class);
                startActivity(intent);
                // kill this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
