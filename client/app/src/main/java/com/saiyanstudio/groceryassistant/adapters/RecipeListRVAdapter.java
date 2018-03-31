package com.saiyanstudio.groceryassistant.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.models.GroceryItem;
import com.saiyanstudio.groceryassistant.models.RecipeItem;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by deeks on 1/29/2016.
 */
public class RecipeListRVAdapter extends RecyclerView.Adapter<RecipeListRVAdapter.RecipeListViewHolder>{

    List<RecipeItem> recipeItemList;
    Context context;

    public RecipeListRVAdapter(Context context,List<RecipeItem> itemList){
        this.context = context;
        this.recipeItemList = itemList;
    }

    @Override
    public RecipeListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_list_item, parent, false);
        RecipeListViewHolder flvh = new RecipeListViewHolder(v);
        return flvh;

    }

    @Override
    public void onBindViewHolder(RecipeListViewHolder holder, int position) {
        holder.recipeName.setText(recipeItemList.get(position).getName());
        holder.likes_Value.setText(recipeItemList.get(position).getLikes() + "");
        if(!(recipeItemList.get(position).getImageURL().equalsIgnoreCase("N/A")) || (recipeItemList.get(position).getImageURL() == null)){
            Picasso.with(context).load(recipeItemList.get(position).getImageURL()).into(holder.recipeIcon);
        }else {
            holder.recipeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.apple));
        }
    }

    @Override
    public int getItemCount() {
        return recipeItemList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class RecipeListViewHolder extends RecyclerView.ViewHolder {

        TextView recipeName;
        TextView likes_Value;
        ImageView recipeIcon;

        public RecipeListViewHolder(View itemView) {
            super(itemView);

            recipeName = (TextView) itemView.findViewById(R.id.recipeName);
            likes_Value = (TextView) itemView.findViewById(R.id.likes_Value);
            recipeIcon = (ImageView) itemView.findViewById(R.id.recipe_iconImageView);
        }

    }

}