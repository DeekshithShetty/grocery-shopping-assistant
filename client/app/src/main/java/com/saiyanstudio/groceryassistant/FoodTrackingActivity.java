package com.saiyanstudio.groceryassistant;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.saiyanstudio.groceryassistant.adapters.FoodsListRVAdapter;
import com.saiyanstudio.groceryassistant.models.GroceryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FoodTrackingActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private TextView dateText;

    private BarChart nutChart;
    private YAxis nutChartYAxis;
    private XAxis nutChartXAxis;
    private Legend legend;

    private HorizontalBarChart calorieChart;

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

    RecyclerView recyclerView;
    private List<GroceryItem> groceryItemList = new ArrayList<>();
    private FoodsListRVAdapter adapter;

    private List<ParseObject> parseObjects;
    private ParseObject pObject;

    Calendar calendar;
    int day,month,year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_tracking);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Food Tracking");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        dateText = (TextView) findViewById(R.id.dateText);

        calendar = Calendar.getInstance();
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        year = calendar.get(Calendar.YEAR);

        calorieChart = (HorizontalBarChart) findViewById(R.id.calorieChart);

        nutChart = (BarChart)findViewById(R.id.macroChart);
        nutChartYAxis = nutChart.getAxisLeft();
        nutChartXAxis = nutChart.getXAxis();
        legend = nutChart.getLegend();

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String todaysDate = df.format(c.getTime());

        findUserTotalNutrients(todaysDate);

        recyclerView = (RecyclerView)findViewById(R.id.foodTrackListRv);
        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new org.solovyev.android.views.llm.LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        //LinearLayoutManager layoutManager = new LinearLayoutManager(FoodTrackingActivity.this);
        recyclerView.setLayoutManager(layoutManager);

        initializeDataForRecyclerView(todaysDate);

        adapter = new FoodsListRVAdapter(FoodTrackingActivity.this,groceryItemList);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(FoodTrackingActivity.this, new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {
                        pObject = parseObjects.get(position);
                        Intent intent = new Intent(FoodTrackingActivity.this, NutrientInfoActivity.class);
                        intent.putExtra("productName", pObject.getString("productName"));
                        intent.putExtra("productImage", pObject.getString("imageURL"));
                        intent.putExtra("energy", pObject.getString("energy"));
                        intent.putExtra("carbohydrates", pObject.getString("carbohydrates"));
                        intent.putExtra("sugars", pObject.getString("sugars"));
                        intent.putExtra("protein", pObject.getString("protein"));
                        intent.putExtra("fats", pObject.getString("fats"));
                        startActivity(intent);
                    }
                })
        );
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

                    //weight in kg = weight in pounds x 0.454
                    //height in cm = height in inches x 2.54
                    //height in cm = height in foots x 30.48

                    //calculating calorie requirements per day
                    //male BMR = 66 + (13.8 x weight in kg.) + (5 x height in cm) - (6.8 x age in years)
                    //female BMR = 655 + (9.6 x weight in kg.) + (1.8 x height in cm) - (4.7 x age in years)
                    //Total Daily Energy Expenditure = BMR x Activity Multiplier
                    // Activity Multiplier -> Sedentary = 1.2, Lightly Active = 1.375, Moderately Active = 1.55, Very Active = 1.725, Extremely Active = 1.9
                    if (gender.equalsIgnoreCase("Male")) {
                        prescribedCalories = (float) ((66 + (13.8 * weight) + (5 * height) - (6.8 * age)) * activityMultiplierForCalories);
                    } else {
                        prescribedCalories = (float) ((655 + (9.6 * weight) + (1.8 * height) - (4.7 * age)) * activityMultiplierForCalories);
                    }

                    //calculating carbohydrates requirements per day
                    //grams = calories/4
                    //carbohydrates per day = (60% of Calories)/4
                    prescribedCarbs = (float) (0.6 * prescribedCalories) / 4;

                    //calculating sugars requirements per day
                    //grams = calories/4
                    //sugars per day = (10% of Calories)/4
                    prescribedSugars = (prescribedCalories / 10) / 4;

                    //calculating protein requirements per day
                    prescribedProtein = (float) ((weight / 0.454) * activityMultiplierForProtein);

                    //calculating fats requirements per day
                    //grams = calories/4
                    //fats per day = (30% of Calories)/4
                    prescribedFats = (float) (0.3 * prescribedCalories) / 4;

                    createCalorieChart();
                    createNutChart();
                }
            }
        });
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

    public void findUserTotalNutrients(String date){
        //remove this during deploy
        //Toast.makeText(FoodTrackingActivity.this, "Graph refreshed", Toast.LENGTH_SHORT).show();

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

        ParseQuery<ParseObject> query = ParseQuery.getQuery("NutrientInfo");
        query.whereEqualTo("username", userEmail);
        query.whereEqualTo("uploadDate", date);
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
                    Toast.makeText(FoodTrackingActivity.this, "Error: " + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createCalorieChart() {

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

        nutChart.getXAxis().setLabelsToSkip(0);
        nutChart.getXAxis().setDrawAxisLine(false);
        nutChart.getXAxis().setDrawGridLines(false);
        nutChart.getAxisRight().setEnabled(false);
        nutChart.getAxisRight().setSpaceTop(1);

        nutChart.setClickable(false);
        nutChart.setNoDataTextDescription(" ");
        nutChart.setHorizontalScrollBarEnabled(true);
        nutChart.setDrawGridBackground(false);
        nutChart.setDrawBarShadow(false);
        nutChart.setTouchEnabled(false);
        nutChart.enableScroll();
        nutChart.setDescription(" ");
        nutChart.getLegend().setEnabled(true);
        nutChart.animateY(2500);

    }

    private void initializeDataForRecyclerView(String date){

        groceryItemList.clear();

        String userEmail;
        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
        }else {
            userEmail = "goku@dbz.com";
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("NutrientInfo");
        query.whereEqualTo("username", userEmail);
        query.whereEqualTo("uploadDate", date);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, com.parse.ParseException e) {
                if (e == null) {
                    parseObjects = objects;
                    for (ParseObject parseObject : objects){
                        groceryItemList.add(new GroceryItem(parseObject.getString("productName"), Float.parseFloat(parseObject.getString("energy")),parseObject.getString("imageURL")));
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    // Something went wrong.
                    Toast.makeText(FoodTrackingActivity.this,"Error: " + e.getMessage().toString(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_food_tracking, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_calender) {
            new DatePickerDialog(FoodTrackingActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int _year, int _month, int _day) {

                    year = _year;
                    month = _month;
                    day = _day;

                    String monthString, dayString;

                    if(day < 10)
                        dayString = "0" + day;
                    else
                        dayString = day + "";

                    if((month + 1) < 10)
                        monthString = "0" + (month + 1);
                    else
                        monthString = month + "";

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                    String todaysDate = df.format(c.getTime());

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, -1);
                    String yesterdaysDate = df.format(cal.getTime());


                    if(todaysDate.equals(dayString + "-" + monthString + "-" + year))
                        dateText.setText("Today");
                    else if(yesterdaysDate.equals(dayString + "-" + monthString + "-" + year))
                        dateText.setText("Yesterday");
                    else
                        dateText.setText(dayString + "/" + monthString + "/" + year);
                    Log.i("DATE", "Date : " + dayString + "-" + monthString + "-" + year);

                    findUserTotalNutrients(dayString + "-" + monthString + "-" + year);
                    initializeDataForRecyclerView(dayString + "-" + monthString + "-" + year);
                }
            },year,month,day).show();
        }

        return super.onOptionsItemSelected(item);
    }
}
