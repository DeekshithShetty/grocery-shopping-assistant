package com.saiyanstudio.groceryassistant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.adapters.FoodsListRVAdapter;
import com.saiyanstudio.groceryassistant.adapters.UserInfoRVAdapter;
import com.saiyanstudio.groceryassistant.models.GroceryItem;
import com.saiyanstudio.groceryassistant.models.UserInfo;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    CollapsingToolbarLayout collapsing_container;
    private Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView headerImageView;
    CircleImageView profileIconIV;

    UserInfoRVAdapter adapter;

    private ArrayList<UserInfo> infoList;

    private FloatingActionButton editProfileFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);

        }

        setContentView(R.layout.activity_user_profile);

        collapsing_container = (CollapsingToolbarLayout) findViewById(R.id.collapsing_container);
        //collapsing_container.setTitle("C M Punk");

        headerImageView = (ImageView) findViewById(R.id.header);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.header);
        Bitmap blurred = blurRenderScript(this,bitmap, 25);
        headerImageView.setImageBitmap(blurred);

        profileIconIV = (CircleImageView) findViewById(R.id.profileIconIV);
        if(ParseUser.getCurrentUser() != null){
            String userGender = ParseUser.getCurrentUser().get("gender").toString();
            if(userGender.equals("Male")){
                Bitmap profileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.profile_male);
                profileIconIV.setImageBitmap(profileBitmap);
            }else {
                Bitmap profileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.profile_female);
                profileIconIV.setImageBitmap(profileBitmap);
            }
        }


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //toolbar.setTitle("C M Punk");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView)findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);

        //initializeData();
        infoList = new ArrayList<>();

        adapter = new UserInfoRVAdapter(UserProfileActivity.this,infoList);
        recyclerView.setAdapter(adapter);

        editProfileFab = (FloatingActionButton) findViewById(R.id.editProfileFab);
        editProfileFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        infoList.clear();
        initializeData();
    }

    private void initializeData(){

        String userEmail;
        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
            collapsing_container.setTitle(ParseUser.getCurrentUser().getUsername());
        }else {
            userEmail = "goku@dbz.com";
            collapsing_container.setTitle("Goku");
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
                   infoList.add(new UserInfo("Name", object.getString("username")));
                   infoList.add(new UserInfo("Height", object.getString("height") + " cm"));
                   infoList.add(new UserInfo("Weight", object.getString("weight") + " kg"));
                   infoList.add(new UserInfo("Age", object.getString("age")));
                   infoList.add(new UserInfo("Gender", object.getString("gender")));
                   infoList.add(new UserInfo("Activity Level", object.getString("activityLvl")));

                   adapter.notifyDataSetChanged();
                   //for debug
                   //Toast.makeText(UserProfileActivity.this, "Refreshed the user profile attributes", Toast.LENGTH_SHORT).show();
               }
           }
        });

        /*
        infoList.add(new UserInfo("Name", "C M Punk"));
        infoList.add(new UserInfo("Daily Energy Advice", "1997 Calories"));
        infoList.add(new UserInfo("Height", "273 cm"));
        infoList.add(new UserInfo("Weight", "120 kg"));
        infoList.add(new UserInfo("Age", "26"));
        infoList.add(new UserInfo("Gender", "Male"));
        infoList.add(new UserInfo("Activity Level", "Moderate"));

        */

    }


    @SuppressLint("NewApi")
    public static Bitmap blurRenderScript (Context context, Bitmap smallBitmap, int radius){
        try {
            smallBitmap = RGB565toARGB888(smallBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bitmap bitmap = Bitmap.createBitmap(
                smallBitmap.getWidth(), smallBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript renderScript = RenderScript.create(context);

        Allocation blurInput = Allocation.createFromBitmap(renderScript, smallBitmap);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
                Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(radius); // radius must be 0 < r <= 25
        blur.forEach(blurOutput);

        blurOutput.copyTo(bitmap);
        renderScript.destroy();

        return bitmap;

    }

    private static Bitmap RGB565toARGB888(Bitmap img) throws Exception {
        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

        //Get JPEG pixels.  Each int is the color values for one pixel.
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        //Create a Bitmap of the appropriate format.
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

        //Set RGB pixels.
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;

    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }
}
