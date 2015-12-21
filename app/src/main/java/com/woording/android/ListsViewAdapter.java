/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package com.woording.android;

import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.woording.android.activity.ListViewActivity;
import com.woording.android.activity.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;

public class ListsViewAdapter extends RecyclerView.Adapter<ListsViewAdapter.ViewHolder> {
    private ArrayList<List> mLists;

    public ListsViewAdapter(ArrayList<List> listNames) {
        mLists = listNames;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ListsViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_lists_list_item, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MainActivity.mDualPane) {
                    // Start intent
                    Intent intent = new Intent(MainActivity.mContext, ListViewActivity.class);
                    intent.putExtra("list", mLists.get(position));
                    MainActivity.mContext.startActivity(intent);
                }
            }
        });
        holder.mTitle.setText(mLists.get(position).mName);
        holder.mSubTitle.setText(App.getAppContext().getString(R.string.list_item_subtitle,
                List.getLanguageName(MainActivity.mContext, mLists.get(position).mLanguage1),
                List.getLanguageName(MainActivity.mContext, mLists.get(position).mLanguage2)));
    }

    public void updateList(List[] lists) {
        mLists.clear();
        mLists.addAll(Arrays.asList(lists));

        // Report that the data changed
        notifyDataSetChanged();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CardView mCardView;
        public TextView mTitle;
        public TextView mSubTitle;

        public ViewHolder(View view) {
            super(view);
            mCardView = (CardView) view.findViewById(R.id.card_view);
            mTitle = (TextView) view.findViewById(R.id.list_item_title);
            mSubTitle = (TextView) view.findViewById(R.id.list_item_subtitle);
        }
    }
}
