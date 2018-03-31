package com.saiyanstudio.groceryassistant.fragments;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.AboutAppActivity;
import com.saiyanstudio.groceryassistant.FoodTrackingActivity;
import com.saiyanstudio.groceryassistant.GroceryListActivity;
import com.saiyanstudio.groceryassistant.LoginOrSignupActivity;
import com.saiyanstudio.groceryassistant.MainActivity;
import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.UserProfileActivity;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by deeks on 11/11/2015.
 */
public class NavigationDrawerFragment extends Fragment {

    private static String TAG = NavigationDrawerFragment.class.getSimpleName();

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private TextView headerUserName;
    private TextView headerUserEmail;
    private CircleImageView profileIconIV;

    public NavigationDrawerFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflating view layout
        View layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);

        //Initializing NavigationView
        navigationView = (NavigationView) layout.findViewById(R.id.navigation_view);

        View nav_header = LayoutInflater.from(getActivity()).inflate(R.layout.navigation_drawer_header, null);

        headerUserName = (TextView) nav_header.findViewById(R.id.headerUserName);
        headerUserEmail = (TextView) nav_header.findViewById(R.id.headerUserEmail);

        if(ParseUser.getCurrentUser() != null){
            headerUserName.setText(ParseUser.getCurrentUser().getUsername());
            headerUserEmail.setText(ParseUser.getCurrentUser().getEmail());
        }else {
            headerUserName.setText("Goku");
            headerUserEmail.setText("goku@dbz.com");
        }

        profileIconIV = (CircleImageView) nav_header.findViewById(R.id.profileIconIV);
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

        navigationView.addHeaderView(nav_header);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
               // if (menuItem.isChecked()) menuItem.setChecked(false);
                //else menuItem.setChecked(true);
                navigationView.getMenu().getItem(0).setChecked(true);

                //Closing drawer on item click
                drawerLayout.closeDrawers();

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()) {

                    case R.id.userProfile:
                        startActivityForResult(new Intent(getActivity(), UserProfileActivity.class), 777);
                        //drawerLayout.closeDrawers();
                        break;

                    case R.id.groceryList:
                        startActivity(new Intent(getActivity(), GroceryListActivity.class));
                        //drawerLayout.closeDrawers();
                        break;

                    case R.id.foodTracking:
                        startActivity(new Intent(getActivity(), FoodTrackingActivity.class));
                        //drawerLayout.closeDrawers();
                        break;

                    case R.id.rateTheApp:
                        Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        try {
                            startActivity(goToMarket);
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getActivity().getPackageName())));
                        }
                        //drawerLayout.closeDrawers();
                        break;
                    case R.id.contactUs:
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("message/rfc822");
                        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"feedback.saiyanstudio@gmail.com"});
                        i.putExtra(Intent.EXTRA_SUBJECT, "Grocery Assist Feedback");
                        //i.putExtra(Intent.EXTRA_TEXT,"Body");
                        try {
                            startActivity(Intent.createChooser(i, "Send Email .."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(getActivity(), "No email clients installed", Toast.LENGTH_SHORT).show();
                        }
                        //drawerLayout.closeDrawers();
                        break;
                    case R.id.about:
                        startActivity(new Intent(getActivity(), AboutAppActivity.class));
                        drawerLayout.closeDrawers();
                        break;

                    case R.id.logout:
                        ParseUser.logOut();
                        Intent intent = new Intent(getActivity(), LoginOrSignupActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        break;
                }
                return true;
            }
        });

        return layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){

            case 777:
                if (resultCode == Activity.RESULT_OK) {

                    //refresh the profile attributes and the graph
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.refreshGraphs();

                }
                break;


        }
    }

    public void setUp(int fragmentId, DrawerLayout _drawerLayout, final Toolbar toolbar) {
        drawerLayout = _drawerLayout;
        drawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.drawerOpen,R.string.drawerClose) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                toolbar.setAlpha(1 - slideOffset / 2);
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });

    }

}
