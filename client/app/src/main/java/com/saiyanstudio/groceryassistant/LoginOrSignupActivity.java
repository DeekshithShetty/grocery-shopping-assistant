package com.saiyanstudio.groceryassistant;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LoginOrSignupActivity extends Activity {

    protected Button loginButton;
    protected Button signUpButton;

    protected Button mainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_or_signup);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        loginButton = (Button) findViewById(R.id.button1);
        signUpButton = (Button) findViewById(R.id.button2);
        mainButton = (Button) findViewById(R.id.button3);

		 // Check for cached user using ParseUser.getCurrentUser()

        if (ParseUser.getCurrentUser() != null) {
            Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else {
            //Toast.makeText(LoginOrSignupActivity.this, "No user", Toast.LENGTH_SHORT).show();
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginOrSignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });

            signUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginOrSignupActivity.this, SignUpActivity.class);
                    startActivity(intent);
                }
            });

            mainButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginOrSignupActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });
        }


    }
}
