package com.saiyanstudio.groceryassistant;

import android.content.Intent;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class NutrientInfoActivity extends AppCompatActivity {

    private Toolbar toolbar;

    TextView productNameText;
    TextView calorieText;
    TextView carboText;
    TextView sugarText;
    TextView proteinText;
    TextView totFatText;
    ImageView foodPic;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrient_info);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Nutrient Information");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        foodPic = (ImageView)findViewById(R.id.foodPic);
        productNameText = (TextView) findViewById(R.id.productNameText);
        calorieText = (TextView) findViewById(R.id.calorieText);
        carboText = (TextView) findViewById(R.id.carboText);
        sugarText = (TextView) findViewById(R.id.sugarText);
        proteinText = (TextView) findViewById(R.id.proteinText);
        totFatText = (TextView) findViewById(R.id.totFatText);

        intent = getIntent();

        if(!intent.getStringExtra("productImage").equalsIgnoreCase("N/A")) {
            Picasso.with(this).load(intent.getStringExtra("productImage")).into(foodPic);
        }else {
            foodPic.setImageDrawable(getResources().getDrawable(R.drawable.apple));
        }

        productNameText.setText(intent.getStringExtra("productName"));
        calorieText.setText(intent.getStringExtra("energy"));
        carboText.setText(intent.getStringExtra("carbohydrates"));
        sugarText.setText(intent.getStringExtra("sugars"));
        proteinText.setText(intent.getStringExtra("protein"));
        totFatText.setText(intent.getStringExtra("fats"));

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;

    }
}
