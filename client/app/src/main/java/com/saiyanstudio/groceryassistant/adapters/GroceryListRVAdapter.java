package com.saiyanstudio.groceryassistant.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.handlers.GroceryListDatabaseHandler;
import com.saiyanstudio.groceryassistant.models.GroceryItem;
import com.saiyanstudio.groceryassistant.models.GroceryListItem;

import java.util.List;

/**
 * Created by deeks on 1/29/2016.
 */
public class GroceryListRVAdapter extends RecyclerView.Adapter<GroceryListRVAdapter.GroceryListViewHolder> {

    List<GroceryListItem> groceryItemList;
    Context context;
    String userEmail;
    private GroceryListDatabaseHandler db;

    public GroceryListRVAdapter(Context context, List<GroceryListItem> itemList) {
        this.context = context;
        this.groceryItemList = itemList;

        db = new GroceryListDatabaseHandler(context);

        if(ParseUser.getCurrentUser() != null){
            userEmail = ParseUser.getCurrentUser().getEmail();
        }else {
            userEmail = "goku@dbz.com";
        }
    }

    @Override
    public GroceryListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.grocery_list_item, parent, false);
        GroceryListViewHolder flvh = new GroceryListViewHolder(v);
        return flvh;

    }

    @Override
    public void onBindViewHolder(GroceryListViewHolder holder, final int position) {
        holder.groceryName.setText(groceryItemList.get(position).getName());
        if(groceryItemList.get(position).getIsChecked() == 0)
            holder.groceryCheckBox.setChecked(false);
        else
            holder.groceryCheckBox.setChecked(true);

        holder.groceryCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    Log.i("GROCERY_ASSIST","isChecked is true");
                    Log.i("GROCERY_ASSIST","Id : " + groceryItemList.get(position).getId());
                    db.updateIsCheckedOfUserGroceryListItem(userEmail, 0, 1, groceryItemList.get(position));
                }else
                    db.updateIsCheckedOfUserGroceryListItem(userEmail, 0, 0, groceryItemList.get(position));
            }
        });

    }

    @Override
    public int getItemCount() {
        return groceryItemList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class GroceryListViewHolder extends RecyclerView.ViewHolder {

        TextView groceryName;
        CheckBox groceryCheckBox;

        public GroceryListViewHolder(View itemView) {
            super(itemView);

            groceryName = (TextView) itemView.findViewById(R.id.groceryName);
            groceryCheckBox = (CheckBox) itemView.findViewById(R.id.groceryCheckBox);
        }

    }
}