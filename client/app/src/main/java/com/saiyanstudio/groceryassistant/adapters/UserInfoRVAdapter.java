package com.saiyanstudio.groceryassistant.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.models.UserInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deeks on 11/11/2015.
 */
public class UserInfoRVAdapter extends RecyclerView.Adapter<UserInfoRVAdapter.UserInfoViewHolder>{

    Context context;
    List<UserInfo> infoList;

    public UserInfoRVAdapter(Context context,ArrayList<UserInfo> infoList){
        this.context = context;
        this.infoList = infoList;
    }

    @Override
    public UserInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_info_item, parent, false);
        UserInfoViewHolder uvh = new UserInfoViewHolder(v);
        return uvh;
    }

    @Override
    public void onBindViewHolder(UserInfoViewHolder holder, int position) {
        holder.infoValue.setText(infoList.get(position).getInfoValue());
        holder.infoKey.setText(infoList.get(position).getInfoKey());
    }

    @Override
    public int getItemCount() {
        return infoList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class UserInfoViewHolder extends RecyclerView.ViewHolder{

        TextView infoValue;
        TextView infoKey;

        public UserInfoViewHolder(View itemView) {
            super(itemView);

            infoValue = (TextView) itemView.findViewById(R.id.infoValueTV);
            infoKey = (TextView) itemView.findViewById(R.id.infoKeyTV);

        }
    }
}
