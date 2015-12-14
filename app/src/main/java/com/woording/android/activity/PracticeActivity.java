/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package com.woording.android.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.TableListViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public class PracticeActivity extends AppCompatActivity
    implements RecognitionListener {

    private final String TAG = "PracticeActivity";

    private List mList;
    private int mAskedLanguage; // 1 = language 1 | 2 = language 2 | 0 = both
    private boolean mCaseSensitive = true;
    private ArrayList<String> mUsedWords = new ArrayList<>();
    private ArrayList<String[]> mWrongWords = new ArrayList<>();
    private String[] mRandomWord = new String[2];
    private int mTotalWords = 0;
    private int mLastUsedPracticeMethod = 0; // 0 = keyboard | 1 = speech

    private TableListViewAdapter recyclerViewAdapter;

    private SpeechRecognizer mSpeech = null;
    private Intent mRecognizerIntent;

    // UI elements
    private EditText mTranslation;
    private TextView mRightWord;
    private Menu mMenu;
    private RecyclerView mRecyclerView;

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
                    mLastUsedPracticeMethod = 0;
                    checkWord();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.next_word).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLastUsedPracticeMethod = 0;
                checkWord();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.wrong_words_list);
        // Setup LinearLayoutManager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        // Setup adapter
        recyclerViewAdapter = new TableListViewAdapter(new ArrayList<String>(), new ArrayList<String>());
        mRecyclerView.setAdapter(recyclerViewAdapter);

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

        enableSpeech();
        if (mLastUsedPracticeMethod == 1) {
            mSpeech.startListening(mRecognizerIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_practice, menu);

        // Save the menu reference
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MenuItem enable = mMenu.findItem(R.id.enable_speech);
        MenuItem disable = mMenu.findItem(R.id.disable_speech);
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
            case R.id.enable_speech:
                enable.setVisible(false);
                disable.setVisible(true);

                mSpeech.startListening(mRecognizerIntent);
                return true;
            case R.id.disable_speech:
                enable.setVisible(true);
                disable.setVisible(false);

                mSpeech.stopListening();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableSpeech() {
        mSpeech = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeech.setRecognitionListener(this);
        mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        switch (mAskedLanguage) {
            case 0:
                if (!mList.mLanguage1.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, List.getLocale(mList.mLanguage1));
                if (!mList.mLanguage2.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, List.getLocale(mList.mLanguage2));
                break;

            case 1:
                if (!mList.mLanguage1.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, List.getLocale(mList.mLanguage1));
                break;
            case 2:
                if (!mList.mLanguage2.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, List.getLocale(mList.mLanguage2));
                break;
        }
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
                mWrongWords.add(new String[]{mRandomWord[mAskedLanguage == 1 ? 1 : 0], mTranslation.getText().toString()});
                mRightWord.setText(mRandomWord[mAskedLanguage == 1 ? 1 : 0]);
                mRightWord.setVisibility(View.VISIBLE);
                Snackbar.make(mTranslation, getString(R.string.error_wrong_translation), Snackbar.LENGTH_LONG).show();

                if (mUsedWords.indexOf(mRandomWord[mAskedLanguage == 1 ? 1 : 0]) >= -1)
                    mUsedWords.remove(mRandomWord[mAskedLanguage == 1 ? 1 : 0]);
            }
        }

        if (mLastUsedPracticeMethod == 1) {
            mSpeech.startListening(mRecognizerIntent);
        }
    }

    private boolean isInputRight(String input, String correctWord) {
        // Check for case sensitivity
        if (!mCaseSensitive || mLastUsedPracticeMethod == 1) {
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
        // Hide toolbar buttons
        mMenu.findItem(R.id.enable_speech).setVisible(false);
        mMenu.findItem(R.id.disable_speech).setVisible(false);

        findViewById(R.id.practice_layout).setVisibility(View.GONE);
        findViewById(R.id.practice_results_layout).setVisibility(View.VISIBLE);
        // Set right percentages
        int rightPercentage = (int) Math.round(100 - ((double) mWrongWords.size() / (double) mTotalWords * 100));
        ((TextView) findViewById(R.id.right_text)).setText(getString(R.string.right_text, rightPercentage));

        // Display the wrong words
        if (mWrongWords.size() > 0) {
            findViewById(R.id.wrong_words_layout).setVisibility(View.VISIBLE);

            recyclerViewAdapter.addItems(mWrongWords);
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int errorCode) {
        Log.d(TAG, "FAILED " + getErrorText(errorCode));
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        mTranslation.setText(matches.get(0));
        mLastUsedPracticeMethod = 1;
        checkWord();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(TAG, "onRmsChanged: " + rmsdB);
    }

    public String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                mMenu.findItem(R.id.enable_speech).setVisible(true);
                mMenu.findItem(R.id.disable_speech).setVisible(false);
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                mMenu.findItem(R.id.enable_speech).setVisible(true);
                mMenu.findItem(R.id.disable_speech).setVisible(false);
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }
}
