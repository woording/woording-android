/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package nl.philipdb.wording;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class PracticeActivity extends AppCompatActivity {

    private List mList;
    private int mAskedLanguage; // 1 = language 1 | 2 = language 2 | 0 = both
    private boolean mCaseSensitive = true;
    private ArrayList<String> mUsedWords = new ArrayList<>();
    private ArrayList<String[]> mWrongWords = new ArrayList<>();
    private String[] mRandomWord = new String[2];
    private int mTotalWords = 0;

    // UI elements
    private EditText mTranslation;
    private TextView mRightWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        // Setup Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup button actions
        mRightWord = (TextView) findViewById(R.id.right_word);
        mTranslation = (EditText) findViewById(R.id.translation);
        mTranslation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.next_word || actionId == EditorInfo.IME_ACTION_GO) {
                    checkWord();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.next_word).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkWord();
            }
        });

        // Load intent extras
        Intent intent = getIntent();
        mList = (List) intent.getSerializableExtra("list");
        mAskedLanguage = intent.getIntExtra("askedLanguage", 1);
        mCaseSensitive = intent.getBooleanExtra("caseSensitive", true);

        // Set asked language
        if (mAskedLanguage != 0) {
            if (mAskedLanguage == 1) ((TextView) findViewById(R.id.language)).setText(List.getLanguageName(this, mList.mLanguage1));
            else if (mAskedLanguage == 2) ((TextView) findViewById(R.id.language)).setText(List.getLanguageName(this, mList.mLanguage2));
        }
        nextWord();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                upIntent.putExtra("list", mList);
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
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void nextWord() {
        // Check if list is done
        if (mUsedWords.size() == mList.mLanguage1Words.size()) {
            showPracticeResults();
            return;
        }

        int randomIndexInt = (int) Math.floor(Math.random() * mList.mLanguage1Words.size());
        mRandomWord = new String[]{mList.mLanguage1Words.get(randomIndexInt), mList.mLanguage2Words.get(randomIndexInt)};
        // Check if word is already used
        if (mUsedWords.indexOf(mRandomWord[0]) > -1) nextWord();
        else mUsedWords.add(mRandomWord[0]);

        // Display
        if (mAskedLanguage != 0 && mAskedLanguage <= 2) {
            ((TextView) findViewById(R.id.word_to_translate)).setText(mRandomWord[mAskedLanguage - 1]);
        }
    }

    private void checkWord() {
        mTotalWords++;
        if (mAskedLanguage == 1 || mAskedLanguage == 2) {
            if (isInputRight(mTranslation.getText().toString(), mRandomWord[mAskedLanguage == 1 ? 1 : 0])) {
                mTranslation.setText("");
                mRightWord.setVisibility(View.GONE);
                nextWord();
            } else {
                mWrongWords.add(new String[]{mTranslation.getText().toString(), mRandomWord[mAskedLanguage == 1 ? 1 : 0]});
                mRightWord.setText(mRandomWord[mAskedLanguage == 1 ? 1 : 0]);
                mRightWord.setVisibility(View.VISIBLE);
                Snackbar.make(mTranslation, getString(R.string.error_wrong_translation), Snackbar.LENGTH_LONG).show();

                if (mUsedWords.indexOf(mRandomWord[mAskedLanguage == 1 ? 1 : 2]) >= -1)
                    mUsedWords.remove(mRandomWord[mAskedLanguage == 1 ? 1 : 2]);
            }
        }
    }

    private boolean isInputRight(String input, String correctWord) {
        // Check for case sensitivity
        if (!mCaseSensitive) {
            input = input.toLowerCase();
            correctWord = correctWord.toLowerCase();
        }

        // Check if the word is right
        if (input.equals(correctWord)) {
            return true;
        } else if (correctWord.split("\\s*[,|/|;]\\s*").length >= 2) {
            String[] inputWordArray = input.split("\\s*[,|/|;]\\s*");
            String[] correctWordArray = correctWord.split("\\s*[,|/|;]\\s*");
            Arrays.sort(inputWordArray);
            Arrays.sort(correctWordArray);

            for (int i = 0; i < inputWordArray.length; i++) {
                if (!inputWordArray[i].equals(correctWordArray[i])) {
                    return false;
                }
            }

            return true;
        } else return false;
    }

    private void showPracticeResults() {
        // Hide keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        findViewById(R.id.practice_layout).setVisibility(View.GONE);
        findViewById(R.id.practice_results_layout).setVisibility(View.VISIBLE);
        // Set right percentages
        int rightPercentage = (int) Math.round(100 - ((double) mWrongWords.size() / (double) mTotalWords * 100));
        ((TextView) findViewById(R.id.right_text)).setText(getString(R.string.right_text, rightPercentage));

        // Display the wrong words
        if (mWrongWords.size() > 0) {
            findViewById(R.id.wrong_words_layout).setVisibility(View.VISIBLE);

            TableLayout wrongWordsTable = (TableLayout) findViewById(R.id.wrong_words);
            for (String[] word : mWrongWords) {
                TableRow tableRow = new TableRow(this);
                tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                tableRow.setOrientation(LinearLayout.HORIZONTAL);

                TextView rightWord = new TextView(this);
                rightWord.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                rightWord.setText(word[1]);
                tableRow.addView(rightWord);

                TextView myInput = new TextView(this);
                myInput.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                myInput.setText(word[0]);
                tableRow.addView(myInput);

                wrongWordsTable.addView(tableRow);
            }
        }
    }

}
