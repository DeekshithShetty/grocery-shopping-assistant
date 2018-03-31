package com.saiyanstudio.groceryassistant;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.adapters.FoodsListRVAdapter;
import com.saiyanstudio.groceryassistant.models.GroceryItem;

import java.util.ArrayList;
import java.util.List;

public class SearchFoodsActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private EditText searchFoodET;
    private Button clearSearchFoodButton;

    RecyclerView recyclerView;

    private List<GroceryItem> groceryItemList = new ArrayList<>();
    private FoodsListRVAdapter adapter;

    private TextView noFoodMsg;
    private ProgressBar searchFoodProgressBar;
    private List<ParseObject> parseObjects;
    private ParseObject pObject;

    private String foodQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_foods);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Search Foods");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        //hide keyboard when the activity opens
        hideKeyboard();

        searchFoodET = (EditText) findViewById(R.id.searchFoodET);
        clearSearchFoodButton = (Button) findViewById(R.id.clearSearchFoodButton);
        Resources res = getResources();
        int newColor = res.getColor(R.color.light_grey);
        clearSearchFoodButton.getBackground().setColorFilter(newColor, PorterDuff.Mode.SRC_ATOP);
        searchFoodET.getCompoundDrawables()[0].setColorFilter(newColor, PorterDuff.Mode.SRC_ATOP);

        searchFoodET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_NEXT) {
                    hideKeyboard();
                    foodQuery = searchFoodET.getText().toString();
                    if (!foodQuery.equals("")) {
                        initializeData(foodQuery);
                        adapter = new FoodsListRVAdapter(SearchFoodsActivity.this, groceryItemList);
                        recyclerView.setAdapter(adapter);
                    }
                    return true;
                }
                return false;
            }
        });
        clearSearchFoodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                searchFoodET.setText("");
            }
        });


        noFoodMsg = (TextView) findViewById(R.id.noFoodMsg);

        searchFoodProgressBar = (ProgressBar) findViewById(R.id.searchFoodProgressBar);

        recyclerView = (RecyclerView)findViewById(R.id.foodsListRv);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(SearchFoodsActivity.this);
        recyclerView.setLayoutManager(llm);

        foodQuery = getIntent().getStringExtra("foodQuery");
        searchFoodET.setText(foodQuery);

        initializeData(foodQuery);
        adapter = new FoodsListRVAdapter(SearchFoodsActivity.this,groceryItemList);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(SearchFoodsActivity.this, new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {

                        hideKeyboard();

                        pObject = parseObjects.get(position);
                        Intent intent = new Intent(SearchFoodsActivity.this, NutrientInfoActivity.class);
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

    private void initializeData(String foodQuery){

        groceryItemList.clear();

        String userEmail;
        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
        }else {
            userEmail = "goku@dbz.com";
        }

        final String foodQuery2 = foodQuery;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("NutrientInfo");
        query.whereEqualTo("username", userEmail);
        query.whereMatches("productName", "(" + foodQuery + ")", "i");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, com.parse.ParseException e) {
                if (e == null) {
                    parseObjects = objects;
                    searchFoodProgressBar.setVisibility(View.GONE);
                    if (parseObjects.size() == 0) {
                        noFoodMsg.setVisibility(View.VISIBLE);
                    }else{
                        noFoodMsg.setVisibility(View.INVISIBLE);
                        for (ParseObject parseObject : objects) {
                            groceryItemList.add(new GroceryItem(parseObject.getString("productName"), Float.parseFloat(parseObject.getString("energy")), parseObject.getString("imageURL")));
                            adapter.notifyDataSetChanged();
                        }
                    }

                } else {
                    // Something went wrong.
                    Toast.makeText(SearchFoodsActivity.this, "Error: " + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        groceryItemList.clear();
        adapter.notifyDataSetChanged();
    }
}
