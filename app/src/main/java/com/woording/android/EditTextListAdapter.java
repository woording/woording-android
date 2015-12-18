package com.woording.android;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;

public class EditTextListAdapter extends RecyclerView.Adapter<EditTextListAdapter.ViewHolder> {

    private ArrayList<String> mLanguage1Words;
    private ArrayList<String> mLanguage2Words;

    public EditTextListAdapter(ArrayList<String> language1Words, ArrayList<String> language2Words) {
        this.mLanguage1Words = language1Words;
        this.mLanguage2Words = language2Words;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public EditTextListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_edit_text_row, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (position < mLanguage1Words.size()) {
            holder.language1Word.setText(mLanguage1Words.get(position));
        }
        if (position < mLanguage2Words.size()) {
            holder.language2Word.setText(mLanguage2Words.get(position));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLanguage1Words.size() + 3;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // UI Elements
        public EditText language1Word;
        public EditText language2Word;

        public ViewHolder(View view) {
            super(view);
            // Setup the variables
            language1Word = (EditText) view.findViewById(R.id.sentence);
            language2Word = (EditText) view.findViewById(R.id.sentence_translation);
        }
    }
}
