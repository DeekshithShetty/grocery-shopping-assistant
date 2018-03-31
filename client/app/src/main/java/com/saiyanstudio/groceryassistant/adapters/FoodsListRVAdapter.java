package com.saiyanstudio.groceryassistant.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.models.GroceryItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deeks on 11/9/2015.
 */
public class FoodsListRVAdapter extends RecyclerView.Adapter<FoodsListRVAdapter.FoodListViewHolder>{

    List<GroceryItem> groceryItemList;
    Context context;

    public FoodsListRVAdapter(Context context,List<GroceryItem> itemList){
        this.context = context;
        this.groceryItemList = itemList;
    }

    @Override
    public FoodListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.foods_list_item, parent, false);
        FoodListViewHolder flvh = new FoodListViewHolder(v);
        return flvh;

    }

    @Override
    public void onBindViewHolder(FoodListViewHolder holder, int position) {
        holder.foodName.setText(groceryItemList.get(position).getName());
        holder.foodCalValue.setText(groceryItemList.get(position).getCalorie() + "");
        if(!(groceryItemList.get(position).getImageURL().equalsIgnoreCase("N/A")) || (groceryItemList.get(position).getImageURL() == null)){
            Picasso.with(context).load(groceryItemList.get(position).getImageURL()).into(holder.foodIcon);
        }else {
            holder.foodIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.apple));
        }
    }

    @Override
    public int getItemCount() {
        return groceryItemList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class FoodListViewHolder extends RecyclerView.ViewHolder {

        CardView foodCard;
        TextView foodName;
        TextView foodCalValue;
        ImageView foodIcon;

        public FoodListViewHolder(View itemView) {
            super(itemView);

            foodCard = (CardView) itemView.findViewById(R.id.foodCard);
            foodName = (TextView) itemView.findViewById(R.id.foodName);
            foodCalValue = (TextView) itemView.findViewById(R.id.food_calValue);
            foodIcon = (ImageView) itemView.findViewById(R.id.food_iconImageView);
        }

    }

}
