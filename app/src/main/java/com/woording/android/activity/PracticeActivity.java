/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
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
import com.woording.android.adapter.TableListViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public class PracticeActivity extends AppCompatActivity
    implements RecognitionListener {

    private final String TAG = "PracticeActivity";

    // Practice method constants
    private enum InputMethod {
        KEYBOARD,
        SPEECH
    }
    // Asked language constants
    public enum AskedLanguage {
        BOTH (0),
        LANGUAGE_1 (1),
        LANGUAGE_2 (2);

        private final int position;

        AskedLanguage(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }
    }

    private List mList;
    private String username;
    // Practice options
    private AskedLanguage mAskedLanguage;
    private AskedLanguage currentAskedLanguage;
    private boolean mCaseSensitive = true;

    private ArrayList<String> mUsedWords = new ArrayList<>();
    private ArrayList<String[]> mWrongWords = new ArrayList<>();
    private String[] mRandomWord = new String[2];
    private int mTotalWords = 0;

    private InputMethod mLastUsedPracticeMethod = InputMethod.KEYBOARD;

    private TableListViewAdapter recyclerViewAdapter;

    private SpeechRecognizer mSpeech = null;
    private Intent mRecognizerIntent;

    // UI elements
    private EditText mTranslation;
    private TextView mRightWord;
    private Menu mMenu;
    private TextView mRightWordsCounter;
    private TextView mWrongWordsCounter;
    private TextView mWordsLeftCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        // Setup Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup button actions
        mTranslation = (EditText) findViewById(R.id.translation);
        mTranslation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.next_word || actionId == EditorInfo.IME_ACTION_GO) {
                    mLastUsedPracticeMethod = InputMethod.KEYBOARD;
                    checkWord();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.next_word).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLastUsedPracticeMethod = InputMethod.KEYBOARD;
                checkWord();
            }
        });

        /** Setup the {@link RecyclerView} */
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.wrong_words_list);
        /** Setup the {@link LinearLayoutManager} */
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        /** Setup the {@link TableListViewAdapter} */
        recyclerViewAdapter = new TableListViewAdapter(new ArrayList<String>(), new ArrayList<String>());
        mRecyclerView.setAdapter(recyclerViewAdapter);

        // Load other UI elements
        mRightWord = (TextView) findViewById(R.id.right_word);
        mRightWordsCounter = (TextView) findViewById(R.id.right_words_counter);
        mWrongWordsCounter = (TextView) findViewById(R.id.wrong_words_counter);
        mWordsLeftCounter = (TextView) findViewById(R.id.words_left_counter);

        // Load intent extras
        Intent intent = getIntent();
        mList = (List) intent.getSerializableExtra("list");
        username = intent.getStringExtra("username");
        mAskedLanguage = (AskedLanguage) intent.getSerializableExtra("askedLanguage");
        mCaseSensitive = intent.getBooleanExtra("caseSensitive", true);

        // Set asked language
        if (mAskedLanguage != AskedLanguage.BOTH) {
            if (mAskedLanguage == AskedLanguage.LANGUAGE_1) {
                ((TextView) findViewById(R.id.language)).setText(List.getLanguageName(this, mList.language1));
                currentAskedLanguage = AskedLanguage.LANGUAGE_1;
            } else if (mAskedLanguage == AskedLanguage.LANGUAGE_2) {
                ((TextView) findViewById(R.id.language)).setText(List.getLanguageName(this, mList.language2));
                currentAskedLanguage = AskedLanguage.LANGUAGE_2;
            }
        }
        setCounters();
        nextWord();

        enableSpeech();
        if (mLastUsedPracticeMethod == InputMethod.SPEECH) {
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
            case BOTH:
                if (!mList.language1.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.language1));
                if (!mList.language2.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.language2));
                break;

            case LANGUAGE_1:
                if (!mList.language1.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.language1));
                break;
            case LANGUAGE_2:
                if (!mList.language2.equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.language2));
                break;
        }
    }

    private void nextWord() {
        // Check if list is done
        if (mUsedWords.size() == mList.getTotalWords() * (mAskedLanguage == AskedLanguage.BOTH ? 2 : 1)) {
            showPracticeResults();
            return;
        }

        // Random choose language and display it
        if (mAskedLanguage == AskedLanguage.BOTH) {
            AskedLanguage[] options = new AskedLanguage[]{AskedLanguage.LANGUAGE_1, AskedLanguage.LANGUAGE_2};
            currentAskedLanguage = options[(int) Math.round(Math.random())];
            if (currentAskedLanguage == AskedLanguage.LANGUAGE_1) {
                ((TextView) findViewById(R.id.language)).setText(List.getLanguageName(this, mList.language1));
            } else if (currentAskedLanguage == AskedLanguage.LANGUAGE_2) {
                ((TextView) findViewById(R.id.language)).setText(List.getLanguageName(this, mList.language2));
            }
        }
        Log.d(TAG, "nextWord: currentAskedLanguage: " + currentAskedLanguage);

        int randomIndexInt = (int) Math.floor(Math.random() * mList.language1Words.size());
        mRandomWord = new String[]{mList.language1Words.get(randomIndexInt), mList.language2Words.get(randomIndexInt)};
        // Check if word is already used
        if (mUsedWords.indexOf(mRandomWord[currentAskedLanguage.getPosition() - 1]) > -1) nextWord();
        else {
            mUsedWords.add(mRandomWord[currentAskedLanguage.getPosition() - 1]);

            // Display
            ((TextView) findViewById(R.id.word_to_translate)).setText(mRandomWord[currentAskedLanguage.getPosition() - 1]);
        }
    }

    private void checkWord() {
        mTotalWords++;
        int position = currentAskedLanguage == AskedLanguage.LANGUAGE_1 ? 1 : 0;
        if (isInputRight(mTranslation.getText().toString(), mRandomWord[position])) {
            mTranslation.setText("");
            mRightWord.setVisibility(View.GONE);
            setCounters();
            nextWord();
        } else {
            mWrongWords.add(new String[]{mRandomWord[position], mTranslation.getText().toString()});
            mRightWord.setText(mRandomWord[position]);
            mRightWord.setVisibility(View.VISIBLE);
            Snackbar.make(mTranslation, getString(R.string.error_wrong_translation), Snackbar.LENGTH_SHORT).show();

            if (mUsedWords.indexOf(mRandomWord[position]) >= -1)
                mUsedWords.remove(mRandomWord[position]);

            setCounters();
        }

        if (mLastUsedPracticeMethod == InputMethod.SPEECH) {
            mSpeech.startListening(mRecognizerIntent);
        }
    }

    private boolean isInputRight(String input, String correctWord) {
        final String PERMISSIBLE_CHARACTERS = "\\(|\\{|\\[|\\]|\\}|\\)";

        // Check for case sensitivity
        if (!mCaseSensitive || mLastUsedPracticeMethod == InputMethod.SPEECH) {
            input = input.toLowerCase();
            correctWord = correctWord.toLowerCase();
        }

        // Remove some specific character, but only if the correct word contains them
        if (correctWord.matches(".*[" + PERMISSIBLE_CHARACTERS + "]")) {
            input = input.replaceAll(PERMISSIBLE_CHARACTERS, "");
            correctWord = correctWord.replaceAll(PERMISSIBLE_CHARACTERS, "");

            // Also remove eventual last space
            if (correctWord.endsWith(" ")) {
                StringBuilder builder = new StringBuilder(correctWord);
                builder.deleteCharAt(correctWord.length() - 1);
                correctWord = builder.toString();
            }
            if (input.endsWith(" ")) {
                StringBuilder builder = new StringBuilder(input);
                builder.deleteCharAt(input.length() - 1);
                input = builder.toString();
            }
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

    private void setCounters() {
        int totalWords = mList.getTotalWords() * (mAskedLanguage == AskedLanguage.BOTH ? 2 : 1);
        mWordsLeftCounter.setText(getString(R.string.current_words_left, totalWords - mUsedWords.size()));
        mWrongWordsCounter.setText(getString(R.string.current_wrong_words, mWrongWords.size()));
        mRightWordsCounter.setText(getString(R.string.current_right_words, mTotalWords - mWrongWords.size()));
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
        mLastUsedPracticeMethod = InputMethod.SPEECH;
        checkWord();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    private String getErrorText(int errorCode) {
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
