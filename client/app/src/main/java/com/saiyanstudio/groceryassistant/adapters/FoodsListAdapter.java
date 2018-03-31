package com.saiyanstudio.groceryassistant.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.models.GroceryItem;

import java.util.List;

/**
 * Created by deeks on 11/8/2015.
 */
public class FoodsListAdapter extends BaseAdapter {

    private Activity activity;
    private LayoutInflater inflater;
    private List<GroceryItem> groceryItems;

    ImageView food_iconImageView;
    TextView food_Name;
    TextView food_calValue;

    public FoodsListAdapter(Activity activity,List<GroceryItem> groceryItems){
        this.activity = activity;
        this.groceryItems = groceryItems;
    }


    @Override
    public int getCount() {
        return groceryItems.size();
    }

    @Override
    public Object getItem(int position) {
        return groceryItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(inflater == null)
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(convertView == null)
            convertView = inflater.inflate(R.layout.foods_list_item,null);

        food_iconImageView = (ImageView) convertView.findViewById(R.id.food_iconImageView);
        food_Name = (TextView) convertView.findViewById(R.id.foodName);
        food_calValue = (TextView) convertView.findViewById(R.id.food_calValue);

        GroceryItem item = groceryItems.get(position);

        food_Name.setText(item.getName());
        food_calValue.setText(item.getCalorie()+ " kcal");

        return convertView;
    }
}
