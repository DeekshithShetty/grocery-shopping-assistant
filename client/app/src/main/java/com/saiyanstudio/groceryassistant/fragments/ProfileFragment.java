package com.saiyanstudio.groceryassistant.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.models.GroceryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by deeks on 11/7/2015.
 */
public class ProfileFragment extends Fragment {

    private BarChart nutChart;
    private YAxis nutChartYAxis;
    private XAxis nutChartXAxis;
    private Legend legend;

    private HorizontalBarChart calorieChart;

    CircleImageView profileIconIV;

    private float userTotCalories;
    private float userTotCarbs;
    private float userTotSugars;
    private float userTotProtein;
    private float userTotFats;

    private float prescribedCalories;
    private float prescribedCarbs;
    private float prescribedSugars;
    private float prescribedProtein;
    private float prescribedFats;

    private TextView userName;
    private TextView userHeight;
    private TextView userWeight;
    private TextView userAge;
    private TextView userGender;
    private TextView userActivityLvl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        calorieChart = (HorizontalBarChart) rootView.findViewById(R.id.chart1);

        nutChart = (BarChart)rootView.findViewById(R.id.chart2);
        nutChartYAxis = nutChart.getAxisLeft();
        nutChartXAxis = nutChart.getXAxis();
        legend = nutChart.getLegend();

        findUserTotalNutrients();

        profileIconIV = (CircleImageView) rootView.findViewById(R.id.profileIconIV);
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

        userName = (TextView) rootView.findViewById(R.id.userName);
        userHeight = (TextView) rootView.findViewById(R.id.userHeight);
        userWeight = (TextView) rootView.findViewById(R.id.userWeight);
        userAge = (TextView) rootView.findViewById(R.id.userAge);
        userGender = (TextView) rootView.findViewById(R.id.userGender);
        userActivityLvl = (TextView) rootView.findViewById(R.id.userActivityLvl);

        return rootView;
    }

    private void findUserPrescribedNutrients() {
        prescribedCalories = 0;
        prescribedCarbs = 0;
        prescribedSugars = 0;
        prescribedProtein = 0;
        prescribedFats = 0;

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
                    String gender = object.getString("gender");
                    float weight = Float.parseFloat(object.getString("weight"));
                    float height = Float.parseFloat(object.getString("height"));
                    int age = Integer.parseInt(object.getString("age"));
                    float activityMultiplierForCalories = getActivityMultiplierForCalories(object.getString("activityLvl"));
                    float activityMultiplierForProtein = getActivityMultiplierForProtein(object.getString("activityLvl"));

                    //set profile attributes
                    userName.setText(object.getString("username"));
                    userHeight.setText(height + " cm");
                    userWeight.setText(weight + " kg");
                    userAge.setText(age + "");
                    userGender.setText(gender);
                    userActivityLvl.setText(getActivityLvlShortForm(object.getString("activityLvl")));

                    //weight in kg = weight in pounds x 0.454
                    //height in cm = height in inches x 2.54
                    //height in cm = height in foots x 30.48

                    //calculating calorie requirements per day
                    //male BMR = 66 + (13.8 x weight in kg.) + (5 x height in cm) - (6.8 x age in years)
                    //female BMR = 655 + (9.6 x weight in kg.) + (1.8 x height in cm) - (4.7 x age in years)
                    //Total Daily Energy Expenditure = BMR x Activity Multiplier
                    // Activity Multiplier -> Sedentary = 1.2, Lightly Active = 1.375, Moderately Active = 1.55, Very Active = 1.725, Extremely Active = 1.9
                    if(gender.equalsIgnoreCase("Male")){
                        prescribedCalories = (float)((66 + (13.8*weight) + (5*height) - (6.8*age)) * activityMultiplierForCalories);
                    }else {
                        prescribedCalories = (float)((655 + (9.6*weight) + (1.8*height) - (4.7*age)) * activityMultiplierForCalories);
                    }

                    //calculating carbohydrates requirements per day
                    //grams = calories/4
                    //carbohydrates per day = (60% of Calories)/4
                    prescribedCarbs = (float)(0.6 * prescribedCalories) / 4;

                    //calculating sugars requirements per day
                    //grams = calories/4
                    //sugars per day = (10% of Calories)/4
                    prescribedSugars = (prescribedCalories/10) / 4;

                    //calculating protein requirements per day
                    prescribedProtein = (float)((weight / 0.454) * activityMultiplierForProtein);

                    //calculating fats requirements per day
                    //grams = calories/4
                    //fats per day = (30% of Calories)/4
                    prescribedFats = (float)(0.3 * prescribedCalories) / 4;

                    createCalorieChart();
                    createNutChart();
                }
            }
        });
    }

    private String getActivityLvlShortForm(String activityLevel){
        String activityLvlShortForm = "";
        switch (activityLevel){
            case "Sedentary":
                activityLvlShortForm = "Sed";
                break;
            case "Lightly Active":
                activityLvlShortForm = "Light";
                break;
            case "Moderately Active":
                activityLvlShortForm = "Mod";
                break;
            case "Very Active":
                activityLvlShortForm = "Very";
                break;
            case "Extremely Active":
                activityLvlShortForm = "Ext";
                break;
        }
        return activityLvlShortForm;
    }

    private float getActivityMultiplierForCalories(String activityLevel){
        float activityMultiplier = 0;
        switch (activityLevel){
            case "Sedentary":
                activityMultiplier = (float)1.2;
                break;
            case "Lightly Active":
                activityMultiplier = (float)1.375;
                break;
            case "Moderately Active":
                activityMultiplier = (float)1.55;
                break;
            case "Very Active":
                activityMultiplier = (float)1.725;
                break;
            case "Extremely Active":
                activityMultiplier = (float)1.9;
                break;
        }
        return activityMultiplier;
    }

    private float getActivityMultiplierForProtein(String activityLevel){
        float activityMultiplier = 0;
        switch (activityLevel){
            case "Sedentary":
                activityMultiplier = (float)0.4;
                break;
            case "Lightly Active":
                activityMultiplier = (float)0.5;
                break;
            case "Moderately Active":
                activityMultiplier = (float)0.6;
                break;
            case "Very Active":
                activityMultiplier = (float)0.75;
                break;
            case "Extremely Active":
                activityMultiplier = (float)0.9;
                break;
        }
        return activityMultiplier;
    }

    public void findUserTotalNutrients(){
        //remove this during deploy
        //Toast.makeText(getActivity(), "Graph refreshed", Toast.LENGTH_SHORT).show();

        userTotCalories = 0;
        userTotCarbs = 0;
        userTotSugars = 0;
        userTotProtein = 0;
        userTotFats = 0;

        String userEmail;
        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
        }else {
            userEmail = "goku@dbz.com";
        }

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String todaysDate = df.format(c.getTime());

        ParseQuery<ParseObject> query = ParseQuery.getQuery("NutrientInfo");
        query.whereEqualTo("username", userEmail);
        query.whereEqualTo("uploadDate", todaysDate);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, com.parse.ParseException e) {
                if (e == null) {

                    for (ParseObject parseObject : objects){
                        userTotCalories += ( Float.parseFloat(parseObject.getString("energy")) * parseObject.getInt("scansPerDay") );
                        userTotCarbs += ( Float.parseFloat(parseObject.getString("carbohydrates")) * parseObject.getInt("scansPerDay") );
                        userTotSugars += ( Float.parseFloat(parseObject.getString("sugars")) * parseObject.getInt("scansPerDay") );
                        userTotProtein += ( Float.parseFloat(parseObject.getString("protein")) * parseObject.getInt("scansPerDay") );
                        userTotFats += ( Float.parseFloat(parseObject.getString("fats")) * parseObject.getInt("scansPerDay") );
                    }
                    findUserPrescribedNutrients();
                } else {
                    // Something went wrong.
                    Toast.makeText(getActivity(), "Error: " + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createCalorieChart() {

        if(!isAdded()){ return; }
        //MPAndroidChart
        calorieChart.clear();
        calorieChart.clearAnimation();

        ArrayList<BarEntry> calorieEntries = new ArrayList<>();

        calorieEntries.add(new BarEntry(new float[]{userTotCalories, prescribedCalories}, 0));

        BarDataSet calorieDataset = new BarDataSet(calorieEntries, "Calorie");
        calorieDataset.setColors(new int[]{getResources().getColor(R.color.primaryColorDark),
                getResources().getColor(R.color.colorAccent)});
        calorieDataset.setValueTextColor(Color.GRAY);
        calorieDataset.setBarSpacePercent(20f);

        ArrayList<String> xVals = new ArrayList<String>();
        xVals.add("Calorie");


        BarData data1 = new BarData(xVals, calorieDataset);
        data1.setValueTextSize(17f);
        data1.setValueTextColor(0xFFEEEEEE);

        calorieChart.setData(data1);
        calorieChart.invalidate();

        calorieChart.getXAxis().setDrawAxisLine(false);
        calorieChart.getXAxis().setDrawGridLines(false);
        calorieChart.getXAxis().setEnabled(false);
        calorieChart.getAxisLeft().setEnabled(false);
        calorieChart.getAxisLeft().setSpaceTop(1);
        calorieChart.getAxisRight().setEnabled(false);
        calorieChart.getAxisRight().setSpaceTop(1);
        calorieChart.setDrawValueAboveBar(false);

        calorieChart.setDescription(null);
        calorieChart.setClickable(false);
        calorieChart.setNoDataTextDescription(" ");
        calorieChart.setHorizontalScrollBarEnabled(true);
        calorieChart.setDrawGridBackground(false);
        calorieChart.setDrawBarShadow(false);
        calorieChart.setTouchEnabled(false);
        calorieChart.enableScroll();
        calorieChart.setDescription(" ");
        calorieChart.getLegend().setEnabled(false);
        calorieChart.animateY(2500);

    }
    private void createNutChart(){

        if(!isAdded()){ return; }

        //MPAndroidChart
        nutChart.clear();
        nutChart.clearAnimation();

        ArrayList<BarEntry> healthyNutEntries = new ArrayList<>();

        healthyNutEntries.add(new BarEntry(new float[]{prescribedCarbs}, 0));
        healthyNutEntries.add(new BarEntry(new float[]{prescribedSugars}, 1));
        healthyNutEntries.add(new BarEntry(new float[]{prescribedProtein}, 2));
        healthyNutEntries.add(new BarEntry(new float[]{prescribedFats}, 3));

        ArrayList<BarEntry> productNutEntries = new ArrayList<>();

        productNutEntries.add(new BarEntry(new float[]{userTotCarbs}, 0));
        productNutEntries.add(new BarEntry(new float[]{userTotSugars}, 1));
        productNutEntries.add(new BarEntry(new float[]{userTotProtein}, 2));
        productNutEntries.add(new BarEntry(new float[]{userTotFats}, 3));

        BarDataSet healthyNutDataset = new BarDataSet(healthyNutEntries, "Healthy Nut");
        healthyNutDataset.setColor(getResources().getColor(R.color.colorAccent));
        healthyNutDataset.setValueTextColor(getResources().getColor(R.color.textColor));

        BarDataSet productNutDataset = new BarDataSet(productNutEntries, "Product Nut");
        productNutDataset.setColor(getResources().getColor(R.color.primaryColorDark));
        productNutDataset.setValueTextColor(getResources().getColor(R.color.textColor));

        ArrayList<String> labels = new ArrayList<String>();

        labels.add("Carbs");
        labels.add("Sugar");
        labels.add("Protein");
        labels.add("Fat");

        //nutChartYAxis.setAxisMinValue(20);
        //nutChartYAxis.setAxisMaxValue(40);
        nutChartXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        nutChartYAxis.setTextColor(getResources().getColor(R.color.textColorDark));
        nutChartXAxis.setTextColor(getResources().getColor(R.color.textColorDark));
        legend.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setTextColor(getResources().getColor(R.color.textColor));

        legend.setTextColor(getResources().getColor(R.color.textColor));


        legend.setFormSize(6f); // set the size of the legend forms/shapes
        legend.setTextSize(12f);

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        dataSets.add(healthyNutDataset);
        dataSets.add(productNutDataset);


        BarData data2 = new BarData(labels, dataSets);
        nutChart.setData(data2);

        data2.setGroupSpace(80f);
/*
        nutChart.getXAxis().setEnabled(false);
        nutChart.getAxisLeft().setEnabled(false);
        nutChart.getAxisLeft().setSpaceTop(1);
*/
        nutChart.getXAxis().setLabelsToSkip(0);
        nutChart.getXAxis().setDrawAxisLine(false);
        nutChart.getXAxis().setDrawGridLines(false);
        nutChart.getAxisRight().setEnabled(false);
        nutChart.getAxisRight().setSpaceTop(1);

        nutChart.setClickable(false);
        nutChart.setNoDataTextDescription(" ");
        //nutChart.setNoDataTextDescription("No data available for nutChart");
        nutChart.setHorizontalScrollBarEnabled(true);
        nutChart.setDrawGridBackground(false);
        nutChart.setDrawBarShadow(false);
        nutChart.setTouchEnabled(false);
        nutChart.enableScroll();
        nutChart.setDescription(" ");
        nutChart.getLegend().setEnabled(true);
        nutChart.animateY(2500);

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }
}
