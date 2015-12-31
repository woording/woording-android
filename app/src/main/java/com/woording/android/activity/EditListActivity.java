package com.woording.android.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.fragment.EditListFragment;

public class EditListActivity extends AppCompatActivity {

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

        final EditListFragment fragment = (EditListFragment) getSupportFragmentManager().findFragmentById(R.id.edit_list_fragment);
        // Load in list
        if (getIntent().getSerializableExtra("list") != null) {
            fragment.loadList((List) getIntent().getSerializableExtra("list"));
        }

        // Setup FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.saveList();
            }
        });
    }

}
