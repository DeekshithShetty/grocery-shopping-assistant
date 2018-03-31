package com.saiyanstudio.groceryassistant.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.parse.ParseUser;
import com.saiyanstudio.groceryassistant.GroceryListEditDialog;
import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.handlers.GroceryListDatabaseHandler;
import com.saiyanstudio.groceryassistant.models.GroceryListItem;

import java.util.List;

/**
 * Created by deeks on 1/29/2016.
 */
public class GroceryListEditRVAdapter extends RecyclerView.Adapter<GroceryListEditRVAdapter.GroceryListViewHolder> {

    List<GroceryListItem> groceryItemList;
    Context context;
    GroceryListEditDialog dialog;
    String userEmail;
    private GroceryListDatabaseHandler db;

    public GroceryListEditRVAdapter(Context context,GroceryListEditDialog dialog, List<GroceryListItem> itemList) {
        this.context = context;
        this.dialog = dialog;
        this.groceryItemList = itemList;

        db = new GroceryListDatabaseHandler(context);

        if (ParseUser.getCurrentUser() != null) {
            userEmail = ParseUser.getCurrentUser().getEmail();
        } else {
            userEmail = "goku@dbz.com";
        }
    }

    @Override
    public GroceryListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.grocery_list_edit_item, parent, false);
        GroceryListViewHolder flvh = new GroceryListViewHolder(v);
        return flvh;

    }

    @Override
    public void onBindViewHolder(GroceryListViewHolder holder, final int position) {

        holder.groceryName.setText(groceryItemList.get(position).getName());

        holder.groceryDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.updateUserGroceryListItem(userEmail, 1, groceryItemList.get(position));
                dialog.removeItemFromGroceryList(position);
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
        Button groceryDelete;

        public GroceryListViewHolder(View itemView) {
            super(itemView);

            groceryName = (TextView) itemView.findViewById(R.id.groceryName);
            groceryDelete = (Button) itemView.findViewById(R.id.groceryDelete);
        }

    }
}