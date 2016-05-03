/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.adapter;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.woording.android.App;
import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.activity.ListViewActivity;
import com.woording.android.activity.MainActivity;
import com.woording.android.fragment.ListViewFragment;
import com.woording.android.fragment.ListsListFragment;
import com.woording.android.util.ConvertLanguage;

import java.util.ArrayList;
import java.util.Arrays;

public class ListsViewAdapter extends RecyclerView.Adapter<ListsViewAdapter.ViewHolder> implements Filterable {
    private ArrayList<List> mLists;
    private ArrayList<List> filteredList;

    private boolean filtered = false;

    public ListsViewAdapter(ArrayList<List> lists) {
        this.mLists = lists;
        this.filteredList = new ArrayList<>();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ListsViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_lists_list_item, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final List list = filtered ? filteredList.get(holder.getAdapterPosition()) : mLists.get(holder.getAdapterPosition());
        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!App.mDualPane) {
                    // Start intent
                    Intent intent = new Intent(MainActivity.mContext, ListViewActivity.class);
                    intent.putExtra("list", list);
                    intent.putExtra("username", ListsListFragment.currentUsername);
                    MainActivity.mContext.startActivity(intent);
                } else {
                    // Display fragment in same activity (Tablet)
                    ListViewFragment fragment = ListViewFragment.newInstance(list, ListsListFragment.currentUsername);
                    FragmentTransaction ft = ((AppCompatActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.second_pane, fragment)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null).commit();
                    // Change the FAB
                    MainActivity.fab.setImageResource(R.drawable.ic_add_white_24dp);
                    MainActivity.fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.newList();
                        }
                    });
                }
            }
        });
        holder.mTitle.setText(list.getName());
        holder.mSubTitle.setText(App.getAppContext().getString(R.string.list_item_subtitle,
                ConvertLanguage.toLang(list.getLanguage1()),
                ConvertLanguage.toLang(list.getLanguage2()))
        );
    }

    public void updateList(List[] lists) {
        clearList();
        mLists.addAll(Arrays.asList(lists));

        // Report that the data changed
        notifyItemRangeChanged(0, mLists.size());
    }

    public void setList(List[] lists) {
        mLists = new ArrayList<>(Arrays.asList(lists));

        notifyItemRangeInserted(0, mLists.size());
    }

    public void clearList() {
        int size = mLists.size();
        mLists.clear();

        notifyItemRangeRemoved(0, size);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return filtered ? filteredList.size() : mLists.size();
    }

    @Override
    public Filter getFilter() {
        return new ListFilter(this, mLists);
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

    private static class ListFilter extends Filter {
        private final ListsViewAdapter adapter;
        private final ArrayList<List> originalList;
        private final ArrayList<List> filteredList;

        public ListFilter(ListsViewAdapter adapter, ArrayList<List> list) {
            super();
            this.adapter = adapter;
            this.originalList = list;
            this.filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (constraint.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                final String filterPattern = constraint.toString().toLowerCase();

                for (final List list : originalList) {
                    if (list.getName().toLowerCase().contains(filterPattern)
                            || ConvertLanguage.toLang(list.getLanguage1()).toLowerCase().contains(filterPattern)
                            || ConvertLanguage.toLang(list.getLanguage2()).toLowerCase().contains(filterPattern)) {
                        filteredList.add(list);
                    }
                }
            }
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            adapter.filteredList.clear();
            adapter.filteredList.addAll((ArrayList<List>) results.values);
            adapter.filtered = true;
            adapter.notifyDataSetChanged();
        }
    }
}
