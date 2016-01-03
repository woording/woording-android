/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.fragment.EditListFragment;

public class EditListActivity extends AppCompatActivity {

    private EditListFragment mEditListFragment;

    private final Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_list);

        // If should be in two-pane mode, finish to return to main activity
        if (getResources().getBoolean(R.bool.is_dual_pane)) {
            finish();
            return;
        }

        // Setup the Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEditListFragment = (EditListFragment) getSupportFragmentManager().findFragmentById(R.id.edit_list_fragment);
        // Load in list
        if (getIntent().getSerializableExtra("list") != null) {
            mEditListFragment.loadList((List) getIntent().getSerializableExtra("list"));
        }

        // Setup FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditListFragment.saveList();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // First check if changes are made
        mEditListFragment.areChangesMade();
        if (mEditListFragment.isModifiedSinceLastSave && !mEditListFragment.isNewList) {
            // Build alertDialog
            mEditListFragment.createAlertDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    // Go intent up
                    mEditListFragment.navigateUp();
                }
            }).create().show();
        } else if (mEditListFragment.isModifiedSinceLastSave) {
            mEditListFragment.createAlertDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    // Navigate up
                    NavUtils.navigateUpFromSameTask((Activity) mContext);
                }
            }).create().show();
        } else if (!mEditListFragment.isNewList) {
            mEditListFragment.navigateUp();
        }
    }

}
