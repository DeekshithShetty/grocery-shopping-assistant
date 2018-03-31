package com.saiyanstudio.groceryassistant;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.adapters.GroceryListEditRVAdapter;
import com.saiyanstudio.groceryassistant.adapters.GroceryListRVAdapter;
import com.saiyanstudio.groceryassistant.handlers.GroceryListDatabaseHandler;
import com.saiyanstudio.groceryassistant.models.GroceryListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deeks on 1/29/2016.
 */
public class GroceryListEditDialog  extends Dialog implements android.view.View.OnClickListener {

    private Context context;
    public Button doneButton;

    RecyclerView recyclerView;
    String userEmail;

    private GroceryListDatabaseHandler db;

    private List<GroceryListItem> groceryItemList = new ArrayList<>();
    private GroceryListEditRVAdapter adapter;

    public GroceryListEditDialog(Context context) {
        super(context);

        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_grocery_list_edit_dialog);

        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
        }else {
            userEmail = "goku@dbz.com";
        }

        db = new GroceryListDatabaseHandler(context);

        recyclerView = (RecyclerView)findViewById(R.id.groceryListRv_dialog);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);

        initializeData();

        adapter = new GroceryListEditRVAdapter(context,this,groceryItemList);
        recyclerView.setAdapter(adapter);

        doneButton = (Button) findViewById(R.id.ok_btn);
        doneButton.setOnClickListener(GroceryListEditDialog.this);
    }

    private void initializeData(){

        Log.d("GROCERYLIST", "Retrieving user grocery list....");
        groceryItemList = db.getGroceryList(userEmail);
        Log.d("GROCERYLIST", "grocery list of size " + groceryItemList.size() + " retrieved successfully....");

    }

    public void removeItemFromGroceryList(int position){
        groceryItemList.remove(position);
        adapter.notifyDataSetChanged();

        GroceryListActivity groceryListActivity = (GroceryListActivity)context;
        groceryListActivity.removeItemFromGroceryList(position);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok_btn:
                dismiss();
                break;
        }
    }
}
