package com.saiyanstudio.groceryassistant;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.adapters.ViewPagerAdapter;
import com.saiyanstudio.groceryassistant.fragments.FoodsFragment;
import com.saiyanstudio.groceryassistant.fragments.NavigationDrawerFragment;
import com.saiyanstudio.groceryassistant.fragments.ProfileFragment;
import com.saiyanstudio.groceryassistant.fragments.ScanFragment;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter pagerAdapter;
    private NavigationDrawerFragment drawerFragment;

    private ProfileFragment profileFragment;
    private ScanFragment scanFragment;


    public static final String TAG = "GROCERY ASSISTANT: " +  MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        if (toolbar != null) {
            toolbar.setTitle("Grocery Assist");
            toolbar.setTitleTextColor(0xFFFFFFFF);
            setSupportActionBar(toolbar);
        }

        drawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer,drawerLayout, toolbar);


        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        viewPager.setCurrentItem(1);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setSelectedTabIndicatorColor(Color.WHITE);
        tabLayout.setSelectedTabIndicatorHeight((int) (2 * getResources().getDisplayMetrics().density));
        tabLayout.setupWithViewPager(viewPager);


    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        scanFragment = new ScanFragment();
        adapter.addFragment(scanFragment, "SCAN");
        profileFragment = new ProfileFragment();
        adapter.addFragment(profileFragment, "PROFILE");
        adapter.addFragment(new FoodsFragment(), "FOODS");
        viewPager.setAdapter(adapter);
    }

    public void callNutrientActivity(String barcode){
        scanFragment.removeUploadCard();
        scanFragment.checkForProductInParse(barcode);
    }

    public void callGroceryNutrientActivity(String groceryName){
        scanFragment.getGroceryItemInfoFromParse(groceryName);
    }

    public void callNutrientActivityWithFoundTrue(String barcode){
        scanFragment.checkForProductInParseWithFoundTrue(barcode);
    }

    public void callScanAndUploadNFT(String tempProductPicUploadsFilename){
        scanFragment.scanAndUploadNFT(tempProductPicUploadsFilename);
    }

    public void callNutrientAlertDialog(String energy, String carbohydrates,String sugars, String protein, String fats){
        scanFragment.showNutrientAlertDialog(energy, carbohydrates, sugars, protein, fats);
    }

    public void showNutInfoProgressBar(){
        scanFragment.nutInfoUploadProgressBar.show();
    }

    public void dismissNutInfoProgressBar(){
        scanFragment.nutInfoUploadProgressBar.dismiss();
    }

    public void dismissProductPicProgressBar(){
        scanFragment.productPicUploadProgressBar.dismiss();
    }

    public void dismissNftUploadProgressBar(){
        scanFragment.nftUploadProgressBar.dismiss();
    }

    public void dismissGroceryPicProgressBar(){
        scanFragment.groceryPicUploadProgressBar.dismiss();
    }

    public void refreshGraphs(){
        profileFragment.findUserTotalNutrients();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (null != searchView) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered
                return true;
            }

            public boolean onQueryTextSubmit(String query) {
                Intent intent = new Intent(MainActivity.this, SearchFoodsActivity.class);
                intent.putExtra("foodQuery",query);
                startActivity(intent);
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.logout_button:
                //Log current user out
                ParseUser.logOut();
                Intent intent = new Intent(this, LoginOrSignupActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;

            case R.id.settings_button:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }
}
