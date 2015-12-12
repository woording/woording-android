package com.woording.android;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class TableListViewAdapter extends RecyclerView.Adapter<TableListViewAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<String> Column1;
    private ArrayList<String> Column2;

    public TableListViewAdapter(Context context, ArrayList<String> column1, ArrayList<String> column2) {
        this.mContext = context;
        this.Column1 = column1;
        this.Column2 = column2;
    }

    public void addItem(String column1, String column2) {
        Column1.add(column1);
        Column2.add(column2);
        notifyDataSetChanged();
    }

    public void addItems(ArrayList<String> column1, ArrayList<String> column2) {
        Column1.addAll(column1);
        Column2.addAll(column2);
        notifyDataSetChanged();
    }

    public void addItems(ArrayList<String[]> columns) {
        for (String[] column : columns) {
            Column1.add(column[0]);
            Column2.add(column[1]);
        }
        notifyDataSetChanged();
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
        if (position == 0) {
            holder.mColumn1.setTypeface(null, Typeface.BOLD);
            holder.mColumn2.setTypeface(null, Typeface.BOLD);
        }

        holder.mColumn1.setText(Column1.get(position));
        holder.mColumn2.setText(Column2.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return Column1.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mColumn1;
        public TextView mColumn2;

        public ViewHolder(View view) {
            super(view);
            mColumn1 = (TextView) view.findViewById(R.id.column_1);
            mColumn2 = (TextView) view.findViewById(R.id.column_2);
        }
    }
}
