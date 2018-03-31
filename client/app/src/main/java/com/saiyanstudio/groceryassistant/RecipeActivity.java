package com.saiyanstudio.groceryassistant;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.saiyanstudio.groceryassistant.adapters.RecipeListRVAdapter;
import com.saiyanstudio.groceryassistant.models.GroceryItem;
import com.saiyanstudio.groceryassistant.models.RecipeItem;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private LinearLayout linearLayout_recipe;

    String recipeJsonData;

    ArrayList<String> ingredientList;

    private TextView noRecipeMsg;
    private ProgressBar recipeProgressBar;
    RecyclerView recyclerView;

    private List<RecipeItem> recipeItems = new ArrayList<>();
    private RecipeListRVAdapter adapter;

    String spoonacularFridgeAPIURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Recipe List");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        linearLayout_recipe = (LinearLayout) findViewById(R.id.linearLayout_recipe);

        noRecipeMsg = (TextView)findViewById(R.id.noRecipeMsg);
        recipeProgressBar = (ProgressBar) findViewById(R.id.recipeProgressBar);

        ingredientList = (ArrayList<String>) getIntent().getSerializableExtra("ingredientList");

        recyclerView = (RecyclerView)findViewById(R.id.recipeListRv);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(RecipeActivity.this, new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {
                        String url = "https://www.google.co.in/search?q=" + recipeItems.get(position).getName().replace(" ","+");
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                })
        );

        initializeData();

        adapter = new RecipeListRVAdapter(this,recipeItems);
        recyclerView.setAdapter(adapter);

    }

    private void initializeData() {

        //food2fork api
        //spoonacularFridgeAPIURL = "http://food2fork.com/api/search?key=eae722f5d2be7fd8fb1a0cd6a9310dbd&q=shredded%20chicken";

        spoonacularFridgeAPIURL = "https://spoonacular-recipe-food-nutrition-v1.p.mashape.com/recipes/findByIngredients?ingredients=";

        String ingredientListString = "";
        for(int j = 0 ; j < ingredientList.size(); j++){
            ingredientListString = ingredientListString + ingredientList.get(j).toLowerCase().replace(" ","+");
            if(j != (ingredientList.size()-1))
                ingredientListString += "%2C";
        }

        //ingredientListString = "apples%2Cflour%2Csugar";

        spoonacularFridgeAPIURL += ingredientListString;

        spoonacularFridgeAPIURL += "&limitLicense=false&number=10&ranking=1";

        Log.i("spoonacularFridgeAPIURL",spoonacularFridgeAPIURL);

        if (isNetworkAvailable()) {


            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(spoonacularFridgeAPIURL)
                    .addHeader("X-Mashape-Key", "SDS3P5crHImshhb4jdBQayo4b77hp1TvcMgjsnNMrYbE6MZuYI")
                    .addHeader("Accept", "application/json")
                    .build();

            /*
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(spoonacularFridgeAPIURL)
                    .build();
            */

            Call call = client.newCall(request);
            call.enqueue(new Callback() {

                @Override
                public void onFailure(Request request, IOException e) { }

                @Override
                public void onResponse(Response response) throws IOException {

                    RecipeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recipeProgressBar.setVisibility(View.GONE);
                        }
                    });

                    try {
                        recipeJsonData = response.body().string();
                        Log.v("RECIPE_ACTIVITY", recipeJsonData);

                        if (response.isSuccessful()) {

                            JSONArray recipeArray = new JSONArray(recipeJsonData);
                            Log.d("RECIPE_ACTIVITY", "recipeArray.length() : " + recipeArray.length());
                            for(int i = 0; i < recipeArray.length(); i++){
                                JSONObject recipeData = recipeArray.getJSONObject(i);
                                Log.d("RECIPE_ACTIVITY", "Recipe Name : " + recipeData.getString("title"));

                                RecipeItem recipeItem = new RecipeItem(recipeData.getString("title"),recipeData.getString("image"), recipeData.getInt("likes"),recipeData.getInt("usedIngredientCount"),recipeData.getInt("usedIngredientCount"));
                                //RecipeItem recipeItem = new RecipeItem(recipeData.getString("title"),recipeData.getString("image_url"),3,(int)recipeData.getDouble("social_rank"));

                                recipeItems.add(recipeItem);
                            }

                            RecipeActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if(recipeItems.size() == 0)
                                        noRecipeMsg.setVisibility(View.VISIBLE);
                                    else
                                        noRecipeMsg.setVisibility(View.INVISIBLE);

                                    adapter.notifyDataSetChanged();
                                }
                            });
                        } else {

                            RecipeActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    noRecipeMsg.setVisibility(View.VISIBLE);
                                    //Toast.makeText(RecipeActivity.this, "Sorry no recipes available", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    } catch (IOException e) {
                        Log.e("RECIPE_ACTIVITY", "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e("RECIPE_ACTIVITY", "Exception caught: ", e);
                    }

                }
            });
        } else {
            Snackbar.make(linearLayout_recipe, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(RecipeActivity.this,"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }

    //function to scheck wheter network is available
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;

    }
}
