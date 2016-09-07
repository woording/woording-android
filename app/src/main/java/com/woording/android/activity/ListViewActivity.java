/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.fragment.ListViewFragment;

public class ListViewActivity extends AppCompatActivity {

    // Some constants
    public static final int NO_WORDS_DATA = 1;
    public static final int DELETED_LIST = 2;
    public static final int LIST_NOT_FOUND = 3;
    public static final int USER_NOT_FOUND = 4;
    public static final int SERVER_ERROR = 5;

    private ListViewFragment mListViewFragment;

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
        mListViewFragment = (ListViewFragment) getSupportFragmentManager().findFragmentById(R.id.list_view_fragment);

        // Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        else throw new RuntimeException("getSupportActionBar() should not be null");
    }

    @Override
    public void onStart() {
        super.onStart();

        // Load username
        String username = getIntent().getStringExtra("username");
        mListViewFragment.setUsername(username);
        // Load List from Intent
        List mList = (List) getIntent().getSerializableExtra("list");
        mListViewFragment.setList(mList);

    }

    @Override
    public void onBackPressed() {
        if (mListViewFragment.mDialog != null) {
            if (mListViewFragment.mDialog.isShowing()) {
                mListViewFragment.mDialog.dismiss();
                return;
            }
        }

        goUp(mListViewFragment.getUsername());
    }

    public void goUp(String username) {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        upIntent.putExtra("username", username);
        if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
            // This activity is NOT part of this app's task, so create a new task
            // when navigating up, with a synthesized back stack.
            TaskStackBuilder.create(this)
                    // Add all of this activity's parents to the back stack
                    .addNextIntentWithParentStack(upIntent)
                    // Navigate up to the closest parent
                    .startActivities();
        } else {
            // This activity is part of this app's task, so simply
            // navigate up to the logical parent activity.
            NavUtils.navigateUpTo(this, upIntent);
        }
    }

    public void goUp(int requestCode, String username) {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        upIntent.putExtra("requestCode", requestCode);
        upIntent.putExtra("username", username);
        if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
            // This activity is NOT part of this app's task, so create a new task
            // when navigating up, with a synthesized back stack.
            TaskStackBuilder.create(this)
                    // Add all of this activity's parents to the back stack
                    .addNextIntentWithParentStack(upIntent)
                    // Navigate up to the closest parent
                    .startActivities();
        } else {
            // This activity is part of this app's task, so simply
            // navigate up to the logical parent activity.
            NavUtils.navigateUpTo(this, upIntent);
        }
    }
}
