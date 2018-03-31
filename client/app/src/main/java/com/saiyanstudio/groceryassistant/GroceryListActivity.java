package com.saiyanstudio.groceryassistant;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.adapters.GroceryListRVAdapter;
import com.saiyanstudio.groceryassistant.handlers.GroceryListDatabaseHandler;
import com.saiyanstudio.groceryassistant.models.GroceryItem;
import com.saiyanstudio.groceryassistant.models.GroceryListItem;

import java.util.ArrayList;
import java.util.List;

public class GroceryListActivity extends AppCompatActivity {

    private Toolbar toolbar;

    String userEmail;
    private GroceryListDatabaseHandler db;

    RecyclerView recyclerView;
    private TextView noGroceryMsg;

    private List<GroceryListItem> groceryItemList = new ArrayList<>();
    private GroceryListRVAdapter adapter;

    Button recipe_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_list);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Grocery List");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryColorDark));
        }

        noGroceryMsg = (TextView) findViewById(R.id.noGroceryMsg);

        recipe_button = (Button) findViewById(R.id.recipe_button);
        recipe_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> ingredientList = new ArrayList<String>();

                for(GroceryListItem item : groceryItemList){
                    ingredientList.add(item.getName());
                }

                Intent intent = new Intent(GroceryListActivity.this, RecipeActivity.class);
                intent.putExtra("ingredientList", ingredientList);
                startActivity(intent);
            }
        });


        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
        }else {
            userEmail = "goku@dbz.com";
        }

        db = new GroceryListDatabaseHandler(this);

        recyclerView = (RecyclerView)findViewById(R.id.groceryListRv);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);

        initializeData();

        adapter = new GroceryListRVAdapter(this,groceryItemList);
        //adapter.setGroceryListDatabaseHandler(db);
        recyclerView.setAdapter(adapter);

    }

    private void initializeData(){

        Log.d("GROCERYLIST", "Retrieving user grocery list....");
        groceryItemList = db.getGroceryList(userEmail);
        Log.d("GROCERYLIST", "grocery list of size " + groceryItemList.size() +" retrieved successfully....");

        if(groceryItemList.size() == 0)
            noGroceryMsg.setVisibility(View.VISIBLE);
        else
            noGroceryMsg.setVisibility(View.INVISIBLE);

    }

    public void removeItemFromGroceryList(int position){
        groceryItemList.remove(position);
        adapter.notifyDataSetChanged();

        if(groceryItemList.size() == 0)
            noGroceryMsg.setVisibility(View.VISIBLE);
        else
            noGroceryMsg.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_grocery_list, menu);
        return true;
    }

    private void newGroceryItemAlertDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("New Grocery Item");
        alert.setMessage("Enter the product name to be added");

        final EditText input = new EditText(this);
        alert.setView(input, 70, 0, 80, 0);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //adding new grocery item to user list
                GroceryListItem lc = new GroceryListItem(input.getText().toString(),0);
                groceryItemList.add(lc);
                db.addGroceryListItem(userEmail, lc);
                adapter.notifyDataSetChanged();
                noGroceryMsg.setVisibility(View.INVISIBLE);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alert.show();
    }

    private void clearGroceryItemAlertDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Clear Grocery List");
        alert.setMessage("Are you sure you want to clear your grocery list?");

        alert.setPositiveButton("Clear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //clearing all grocery item list of the user
                List<GroceryListItem> groceryItemListCopy = new ArrayList<>(groceryItemList.size());

                for (GroceryListItem gl: groceryItemList) {
                    try {
                        groceryItemListCopy.add((GroceryListItem)gl.clone());
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
                groceryItemList.clear();
                adapter.notifyDataSetChanged();

                noGroceryMsg.setVisibility(View.VISIBLE);

                for (GroceryListItem gLCopy: groceryItemListCopy) {
                    db.updateUserGroceryListItem(userEmail,1, gLCopy);
                }

                //Toast.makeText(GroceryListActivity.this,"List cleared",Toast.LENGTH_SHORT).show();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {

            //adding new grocery item to user list
            newGroceryItemAlertDialog();

        }else if(id == R.id.action_edit){
            GroceryListEditDialog groceryListEditDialog = new GroceryListEditDialog(GroceryListActivity.this);
            groceryListEditDialog.show();

        }else if(id == R.id.action_clear) {

            clearGroceryItemAlertDialog();
        }

        return super.onOptionsItemSelected(item);
    }
}
