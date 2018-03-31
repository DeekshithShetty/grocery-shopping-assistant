package com.saiyanstudio.groceryassistant;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends Activity {

    TextInputLayout nameWrapper;
    TextInputLayout emailWrapper;
    TextInputLayout passwordWrapper;
    TextInputLayout heightWrapper;
    TextInputLayout weightWrapper;
    TextInputLayout ageWrapper;
    TextInputLayout genderWrapper;

    private String gender;
    private String gender_array[];
    private Spinner genderSpinner;
    private String activityLvl;
    private String activityLvl_array[];
    private Spinner activityLvlSpinner;

    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9#_~!$&'()*+,;=:.\"(),:;<>@\\[\\]\\\\]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*$";
    private Pattern pattern;
    private Matcher matcher;

    protected Button signUpButton;
    protected Button loginButton;
    protected ProgressBar progressBar;
    protected ScrollView scrollView;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        //spinner for gender
        gender_array = new String[2];
        gender_array[0] = "Male";
        gender_array[1] = "Female";
        genderSpinner = (Spinner) findViewById(R.id.gender_spinner);
        ArrayAdapter genderAdapter = new ArrayAdapter(this, R.layout.spinner_item, gender_array);
        genderSpinner.setAdapter(genderAdapter);
        gender = genderSpinner.getSelectedItem().toString();
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                gender = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                gender = adapterView.getSelectedItem().toString();
            }
        });

        //spinner for activity level
        activityLvl_array = new String[5];
        activityLvl_array[0] = "Sedentary";
        activityLvl_array[1] = "Lightly Active";
        activityLvl_array[2] = "Moderately Active";
        activityLvl_array[3] = "Very Active";
        activityLvl_array[4] = "Extremely Active";
        activityLvlSpinner = (Spinner) findViewById(R.id.activityLvl_spinner);
        ArrayAdapter activityLvlAdapter = new ArrayAdapter(this, R.layout.spinner_item, activityLvl_array);
        activityLvlSpinner.setAdapter(activityLvlAdapter);
        activityLvl = activityLvlSpinner.getSelectedItem().toString();
        activityLvlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                activityLvl = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                activityLvl = adapterView.getSelectedItem().toString();
            }
        });


        scrollView = (ScrollView) findViewById(R.id.scrollView);

        nameWrapper = (TextInputLayout) findViewById(R.id.nameWrapper);
        emailWrapper = (TextInputLayout) findViewById(R.id.emailWrapper);
        passwordWrapper = (TextInputLayout) findViewById(R.id.passwordWrapper);
        heightWrapper = (TextInputLayout) findViewById(R.id.heightWrapper);
        weightWrapper = (TextInputLayout) findViewById(R.id.weightWrapper);
        ageWrapper = (TextInputLayout) findViewById(R.id.ageWrapper);

        nameWrapper.setHint("Name");
        emailWrapper.setHint("Email");
        passwordWrapper.setHint("Password");
        heightWrapper.setHint("Height");
        weightWrapper.setHint("Weight");
        ageWrapper.setHint("Age");

        pattern = Pattern.compile(EMAIL_PATTERN);

        //progressBar = (ProgressBar) findViewById(R.id.progressBar1);

        loginButton = (Button) findViewById(R.id.loginBtn);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                // kill this activity
                finish();
            }
        });

        signUpButton = (Button) findViewById(R.id.signUpBtn);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                showProgressDialog();

                String name = nameWrapper.getEditText().getText().toString();
                String email = emailWrapper.getEditText().getText().toString();
                String password = passwordWrapper.getEditText().getText().toString();
                String height = heightWrapper.getEditText().getText().toString();
                String weight = weightWrapper.getEditText().getText().toString();
                String age = ageWrapper.getEditText().getText().toString();



                if (!validateEmail(email)) {
                    emailWrapper.setError("Not a valid email address!");
                    progressDialog.dismiss();
                } else if (!validatePassword(password)) {
                    passwordWrapper.setError("Keep password > 5 chars!");
                    progressDialog.dismiss();
                }else if(name.equals("") || height.equals("") || weight.equals("") || age.equals("")){
                    progressDialog.dismiss();
                    Toast.makeText(SignUpActivity.this, "Please fill all the details!", Toast.LENGTH_SHORT).show();
                } else {

                    if (isNetworkAvailable()) {
                        emailWrapper.setErrorEnabled(false);
                        passwordWrapper.setErrorEnabled(false);

                        //Sign up using ParseUser
                        ParseUser user = new ParseUser();
                        user.setUsername(name);
                        user.setEmail(email);
                        user.setPassword(password);
                        user.put("height", height);
                        user.put("weight", weight);
                        user.put("age", age);
                        user.put("gender", gender);
                        user.put("activityLvl", activityLvl);

                        user.signUpInBackground(new SignUpCallback() {
                            public void done(ParseException e) {
                                if (e == null) {
                                    progressDialog.dismiss();
                                    // Sign up success, open Main Activity
                                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                } else {
                                    // Sign up didn't succeed. Look at the
                                    // ParseException to figure out what went wrong
                                    progressDialog.dismiss();
                                    Snackbar.make(scrollView, "Sign up failed! Try again", Snackbar.LENGTH_LONG).show();
                                    Toast.makeText(SignUpActivity.this, "ParseException : " + e, Toast.LENGTH_LONG).show();
                                    //Toast.makeText(SignUpActivity.this, "Sign up failed! Try again.", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Snackbar.make(scrollView, "No internet connection", Snackbar.LENGTH_LONG).show();
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
        progressDialog = new ProgressDialog(SignUpActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
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
