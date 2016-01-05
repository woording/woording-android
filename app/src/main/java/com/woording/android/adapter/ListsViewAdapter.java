/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.adapter;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.woording.android.App;
import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.activity.ListViewActivity;
import com.woording.android.activity.MainActivity;
import com.woording.android.fragment.ListViewFragment;
import com.woording.android.fragment.ListsListFragment;

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
                if (!App.mDualPane) {
                    // Start intent
                    Intent intent = new Intent(MainActivity.mContext, ListViewActivity.class);
                    intent.putExtra("list", mLists.get(position));
                    intent.putExtra("username", ListsListFragment.currentUsername);
                    MainActivity.mContext.startActivity(intent);
                } else {
                    // Display fragment in same activity (Tablet)
                    ListViewFragment fragment = ListViewFragment.newInstance(mLists.get(position), ListsListFragment.currentUsername);
                    FragmentTransaction ft = ((AppCompatActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.second_pane, fragment)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null).commit();
                    // Change the FAB
                    MainActivity.fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.mContext, R.drawable.ic_add_white_24dp));
                    MainActivity.fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.newList();
                        }
                    });
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
        public final CardView mCardView;
        public final TextView mTitle;
        public final TextView mSubTitle;

        public ViewHolder(View view) {
            super(view);
            mCardView = (CardView) view.findViewById(R.id.card_view);
            mTitle = (TextView) view.findViewById(R.id.list_item_title);
            mSubTitle = (TextView) view.findViewById(R.id.list_item_subtitle);
        }
    }
}
