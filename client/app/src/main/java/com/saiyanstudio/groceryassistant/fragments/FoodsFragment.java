package com.saiyanstudio.groceryassistant.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.EditProfileActivity;
import com.saiyanstudio.groceryassistant.NutrientInfoActivity;
import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.RecyclerItemClickListener;
import com.saiyanstudio.groceryassistant.SearchFoodsActivity;
import com.saiyanstudio.groceryassistant.adapters.FoodsListAdapter;
import com.saiyanstudio.groceryassistant.adapters.FoodsListRVAdapter;
import com.saiyanstudio.groceryassistant.models.GroceryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by deeks on 11/8/2015.
 */
public class FoodsFragment extends Fragment {

    RecyclerView recyclerView;
    FloatingActionButton floatSearchButton;

    private List<GroceryItem> groceryItemList = new ArrayList<>();
    private FoodsListRVAdapter adapter;

    private TextView noFoodMsg;
    private ProgressBar foodProgressBar;
    private List<ParseObject> parseObjects;
    private ParseObject pObject;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_foods, container, false);

        floatSearchButton = (FloatingActionButton)rootView.findViewById(R.id.floatSearchButton);
        floatSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchFoodsActivity.class);
                startActivity(intent);
            }
        });

        noFoodMsg = (TextView) rootView.findViewById(R.id.noFoodMsg);
        foodProgressBar = (ProgressBar) rootView.findViewById(R.id.foodProgressBar);

        recyclerView = (RecyclerView)rootView.findViewById(R.id.foodsListRv);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(llm);

        initializeData();
        adapter = new FoodsListRVAdapter(getActivity(),groceryItemList);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {
                        pObject = parseObjects.get(position);
                        Intent intent = new Intent(getActivity(), NutrientInfoActivity.class);
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

        return rootView;
    }

    private void initializeData(){

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
                    parseObjects = objects;
                    foodProgressBar.setVisibility(View.GONE);

                    if(parseObjects.size() == 0){
                        noFoodMsg.setVisibility(View.VISIBLE);
                    }else{
                        noFoodMsg.setVisibility(View.INVISIBLE);
                        for (ParseObject parseObject : objects){
                            groceryItemList.add(new GroceryItem(parseObject.getString("productName"), Float.parseFloat(parseObject.getString("energy")),parseObject.getString("imageURL")));
                            adapter.notifyDataSetChanged();
                        }
                    }

                } else {
                    // Something went wrong.
                    if(isAdded())
                        Toast.makeText(getActivity(),"Error: " + e.getMessage().toString(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        /*
        //dummy data
        groceryItemList.add(new GroceryItem("Kellogs Corn Flakes", 562));
        groceryItemList.add(new GroceryItem("MTR Badam Mix",456));
        */
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        groceryItemList.clear();
        adapter.notifyDataSetChanged();
    }
}
