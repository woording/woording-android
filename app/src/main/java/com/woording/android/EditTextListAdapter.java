package com.woording.android;

import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;

public class EditTextListAdapter extends RecyclerView.Adapter<EditTextListAdapter.ViewHolder> {

    public ArrayList<String> mLanguage1Words;
    public ArrayList<String> mLanguage2Words;

    public EditTextListAdapter(ArrayList<String> language1Words, ArrayList<String> language2Words) {
        this.mLanguage1Words = language1Words;
        this.mLanguage2Words = language2Words;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public EditTextListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_edit_text_row, parent, false);

        return new ViewHolder(v, new Language1Watcher(), new Language2Watcher());
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.language1Watcher.updatePosition(position);
        holder.language2Watcher.updatePosition(position);

        if (position < mLanguage1Words.size()) {
            holder.language1Word.setText(mLanguage1Words.get(position));
        } else holder.language1Word.setText(null);
        if (position < mLanguage2Words.size()) {
            holder.language2Word.setText(mLanguage2Words.get(position));
        } else holder.language2Word.setText(null);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLanguage1Words.size() + 1;
    }

    public void setWords(ArrayList<String> language1Words, ArrayList<String> language2Words) {
        mLanguage1Words = language1Words;
        mLanguage2Words = language2Words;

        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // UI Elements
        public EditText language1Word;
        public EditText language2Word;
        // Text watchers
        public Language1Watcher language1Watcher;
        public Language2Watcher language2Watcher;

        public ViewHolder(View view, Language1Watcher language1Watcher, Language2Watcher language2Watcher) {
            super(view);
            // Setup the variables
            language1Word = (EditText) view.findViewById(R.id.sentence);
            language2Word = (EditText) view.findViewById(R.id.sentence_translation);
            this.language1Watcher = language1Watcher;
            this.language2Watcher = language2Watcher;
            // Setup the listeners
            language1Word.addTextChangedListener(language1Watcher);
            language2Word.addTextChangedListener(language2Watcher);
        }
    }

    private class Language1Watcher implements TextWatcher {
        private int position;

        public void updatePosition(int position) {
            this.position = position;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mLanguage1Words.size() > position) {
                mLanguage1Words.remove(position);
            } else if (charSequence.length() == 0) return;
            mLanguage1Words.add(position, charSequence.toString());

            while (mLanguage1Words.size() > mLanguage2Words.size()) {
                mLanguage2Words.add(null);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // Do nothing
        }
    }

    private class Language2Watcher implements TextWatcher {
        private int position;

        public void updatePosition(int position) {
            this.position = position;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (mLanguage2Words.size() > position) {
                mLanguage2Words.remove(position);
            } else if (charSequence.length() == 0) return;
            mLanguage2Words.add(position, charSequence.toString());

            while (mLanguage2Words.size() > mLanguage1Words.size()) {
                mLanguage1Words.add(null);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // Do nothing
        }
    }
}
