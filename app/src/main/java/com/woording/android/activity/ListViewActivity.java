/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package com.woording.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.fragment.ListViewFragment;

public class ListViewActivity extends AppCompatActivity {

    public static final int NO_WORDS_DATA = 1;
    public static final int DELETED_LIST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        // If should be in two-pane mode, finish to return to main activity
        if (getResources().getBoolean(R.bool.is_dual_pane)) {
            finish();
            return;
        }

        // Setup fragment
        ListViewFragment mListViewFragment = (ListViewFragment) getSupportFragmentManager().findFragmentById(R.id.list_view_fragment);
        // Load List from Intent
        List mList = (List) getIntent().getSerializableExtra("list");
        mListViewFragment.setList(mList);

        // Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void finishActivity(int requestCode) {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        upIntent.putExtra("requestCode", requestCode);
        startActivity(upIntent);
        finish();
    }
}
