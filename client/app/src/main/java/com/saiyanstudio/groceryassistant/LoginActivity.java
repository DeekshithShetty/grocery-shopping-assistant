package com.saiyanstudio.groceryassistant;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends Activity {

    protected LinearLayout linearLoginLayout;
    protected EditText emailField;
    protected EditText passwordField;
    protected Button loginButton;
    protected Button signUpButton;
    protected ProgressBar progressBar;

    TextInputLayout emailWrapper;
    TextInputLayout passwordWrapper;

    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9#_~!$&'()*+,;=:.\"(),:;<>@\\[\\]\\\\]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*$";
    private Pattern pattern;
    private Matcher matcher;

    ProgressDialog progressDialog;

    final String password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        linearLoginLayout = (LinearLayout) findViewById(R.id.linearLoginLayout);

        emailWrapper = (TextInputLayout) findViewById(R.id.emailWrapper);
        passwordWrapper = (TextInputLayout) findViewById(R.id.passwordWrapper);

        emailWrapper.setHint("Email");
        passwordWrapper.setHint("Password");

        pattern = Pattern.compile(EMAIL_PATTERN);

        emailField = (EditText) findViewById(R.id.email);
        passwordField = (EditText) findViewById(R.id.password);
        loginButton = (Button) findViewById(R.id.loginBtn);
        signUpButton = (Button) findViewById(R.id.signUpBtn);
        //progressBar = (ProgressBar) findViewById(R.id.progressBar1);


        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
                // kill this activity
                finish();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //progressBar.setVisibility(View.VISIBLE);

                hideKeyboard();
                showProgressDialog();

                String email = emailWrapper.getEditText().getText().toString();
                //password = passwordWrapper.getEditText().getText().toString();

                if (!validateEmail(email)) {
                    emailWrapper.getEditText().setError("Not a valid email address!");
                    progressDialog.dismiss();
                } else if (!validatePassword(passwordWrapper.getEditText().getText().toString())) {
                    passwordWrapper.setError("Not a valid password!");
                    progressDialog.dismiss();
                } else {


                    if(isNetworkAvailable()) {
                        emailWrapper.setErrorEnabled(false);
                        passwordWrapper.setErrorEnabled(false);

                        // Login using ParseUser
                        ParseQuery<ParseUser> query = ParseUser.getQuery();
                        query.whereEqualTo("email", email);
                        query.getFirstInBackground(new GetCallback<ParseUser>() {
                           @Override
                           public void done(ParseUser object, ParseException e) {
                               if(object == null){
                                   progressDialog.dismiss();
                                   Snackbar.make(linearLoginLayout, "Sorry you dont have an account. Feel free to Sign up", Snackbar.LENGTH_LONG).show();
                                   Log.d("PARSE", "The getFirst request failed.");
                               }else{
                                   String actualUsername = object.get("username").toString();
                                   //Toast.makeText(LoginActivity.this, actualUsername + ", " + passwordWrapper.getEditText().getText().toString(), Toast.LENGTH_SHORT).show();
                                   ParseUser.logInInBackground(actualUsername, passwordWrapper.getEditText().getText().toString(), new LogInCallback() {
                                       public void done(ParseUser user, ParseException e) {
                                           //progressBar.setVisibility(View.INVISIBLE);
                                           if (user != null) {
                                               // Login successful
                                               progressDialog.dismiss();
                                               Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                               intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                               //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                               startActivity(intent);
                                           } else {
                                               // Login failed. Look at the
                                               // ParseException to see what happened.
                                               progressDialog.dismiss();
                                               Snackbar.make(linearLoginLayout, "Login failed! Please try again", Snackbar.LENGTH_LONG).show();
                                               Log.e("PARSE",e + "");

                                               //Toast.makeText(LoginActivity.this, "Login failed! Please try again.", Toast.LENGTH_LONG).show();
                                           }
                                       }
                                   });
                               }
                           }
                       });
                    }else{
                        progressDialog.dismiss();
                        Snackbar.make(linearLoginLayout, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
                    }
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {

            isAvailable = true;
        }
        return isAvailable;
    }

    public boolean validateEmail(String email) {
        matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public boolean validatePassword(String password) {
        return password.length() > 5;
    }


    private void showProgressDialog(){
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

}
