package com.saiyanstudio.groceryassistant;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.saiyanstudio.groceryassistant.models.UserInfo;

public class EditProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private LinearLayout linearLayout_editProfile;

    private EditText heightValueET;
    private EditText weightValueET;
    private EditText ageValueET;

    private String activityLvl;
    private String activityLvl_array[];
    private Spinner activityLvlSpinner;
    ArrayAdapter activityLvlAdapter;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Edit Profile");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        linearLayout_editProfile = (LinearLayout) findViewById(R.id.linearLayout_editProfile);

        heightValueET = (EditText) findViewById(R.id.heightValueET);
        weightValueET = (EditText) findViewById(R.id.weightValueET);
        ageValueET = (EditText) findViewById(R.id.ageValueET);


        //spinner for activity level
        activityLvl_array = new String[5];
        activityLvl_array[0] = "Sedentary";
        activityLvl_array[1] = "Lightly Active";
        activityLvl_array[2] = "Moderately Active";
        activityLvl_array[3] = "Very Active";
        activityLvl_array[4] = "Extremely Active";
        activityLvlSpinner = (Spinner) findViewById(R.id.activityLvlValueSpinner);
        activityLvlAdapter = new ArrayAdapter(this, R.layout.spinner_item, activityLvl_array);
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


        String userEmail;
        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
        }else {
            userEmail = "goku@dbz.com";
        }

        // Retrieve current user info
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("email", userEmail);
        query.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser object, ParseException e) {
                if (object == null) {
                    Log.d("PARSE", "The getFirst request failed.");
                } else {

                    //set profile attributes
                    heightValueET.setText(object.getString("height"));
                    weightValueET.setText(object.getString("weight"));
                    ageValueET.setText(object.getString("age"));
                    int spinnerPosition = activityLvlAdapter.getPosition(object.getString("activityLvl"));
                    activityLvlSpinner.setSelection(spinnerPosition);

                }
            }
        });
    }

    private void showProgressDialog(){
        progressDialog = new ProgressDialog(EditProfileActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Updating Profile...");
        progressDialog.show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private int getIndexOfActivityLevel(String myActivityLvl){
        int index = 0;
        switch (myActivityLvl){
            case "Sedentary":
                index = 0;
                break;
            case "Lightly Active":
                index = 1;
                break;
            case "Moderately Active":
                index = 2;
                break;
            case "Very Active":
                index = 3;
                break;
            case "Extremely Active":
                index = 4;
                break;
        }
        return index;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {

            hideKeyboard();
            showProgressDialog();

            String userEmail;
            if(ParseUser.getCurrentUser() != null){
                userEmail = ParseUser.getCurrentUser().getEmail();
            }else {
                userEmail = "goku@dbz.com";
            }

            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("email", userEmail);
            query.getFirstInBackground(new GetCallback<ParseUser>() {
                @Override
                public void done(ParseUser object, ParseException e) {
                    if (object == null) {
                        Log.d("PARSE", "The getFirst request failed.");
                    } else {

                        //update profile attributes
                        object.put("height", heightValueET.getText().toString());
                        object.put("weight", weightValueET.getText().toString());
                        object.put("age", ageValueET.getText().toString());
                        object.put("activityLvl", activityLvl);
                        object.saveInBackground(new SaveCallback() {
                            public void done(ParseException e) {
                                if (e == null) {
                                    progressDialog.dismiss();
                                    Snackbar.make(linearLayout_editProfile, "Successfully updated your profile", Snackbar.LENGTH_LONG).show();
                                    //Toast.makeText(EditProfileActivity.this, "Successfully updated your profile", Toast.LENGTH_SHORT).show();
                                } else {
                                    progressDialog.dismiss();
                                    Log.e("PARSE SAVE",e +"");
                                    Snackbar.make(linearLayout_editProfile,"Couldnt update.. Please try again", Snackbar.LENGTH_LONG).show();
                                    //Toast.makeText(EditProfileActivity.this, "Couldnt update.. Please try again", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            });

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
