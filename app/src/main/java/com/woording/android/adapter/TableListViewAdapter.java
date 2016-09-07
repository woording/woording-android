/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.woording.android.R;

import java.util.ArrayList;

public class TableListViewAdapter extends RecyclerView.Adapter<TableListViewAdapter.ViewHolder> {

    private ArrayList<String> mColumn1;
    private ArrayList<String> mColumn2;

    public TableListViewAdapter(ArrayList<String> column1, ArrayList<String> column2) {
        this.mColumn1 = column1;
        this.mColumn2 = column2;
    }

    public void setItems(ArrayList<String> column1, ArrayList<String> column2) {
        mColumn1 = column1;
        mColumn2 = column2;
        notifyItemRangeChanged(0, column1.size() - 1);
    }

    /**
     * Add item to the table view
     * @param column1 Item for the left column
     * @param column2 Item for the right column
     */
    public void addItem(String column1, String column2) {
        mColumn1.add(column1);
        mColumn2.add(column2);
        notifyItemInserted(getItemCount() - 1);
    }

    /**
     * Add items to the table view
     * @param column1 Items for the left column
     * @param column2 Items for the right column
     */
    public void addItems(ArrayList<String> column1, ArrayList<String> column2) {
        int oldLength = getItemCount();
        mColumn1.addAll(column1);
        mColumn2.addAll(column2);
        notifyItemRangeInserted(oldLength, column1.size() - 1);
    }

    public void addItems(ArrayList<String[]> columns) {
        int oldLength = getItemCount();
        for (String[] column : columns) {
            mColumn1.add(column[0]);
            mColumn2.add(column[1]);
        }
        notifyItemRangeInserted(oldLength, columns.size() - 1);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TableListViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_table_row_list_item, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mColumn1.setText(mColumn1.get(position));
        holder.mColumn2.setText(mColumn2.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mColumn1.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mColumn1;
        public final TextView mColumn2;

        public ViewHolder(View view) {
            super(view);
            mColumn1 = (TextView) view.findViewById(R.id.column_1);
            mColumn2 = (TextView) view.findViewById(R.id.column_2);
        }
    }
}
