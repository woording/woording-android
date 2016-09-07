/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.activity;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
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
import android.widget.Toast;

import com.woording.android.List;
import com.woording.android.R;
import com.woording.android.adapter.TableListViewAdapter;
import com.woording.android.util.ConvertLanguage;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;

public class PracticeActivity extends AppCompatActivity
    implements RecognitionListener {

    private static final String TAG = "PracticeActivity";
    public static final int RECORD_AUDIO = 1;

    // Practice method constants
    private enum InputMethod {
        KEYBOARD,
        SPEECH
    }
    // Asked language constants
    public enum AskedLanguage {
        BOTH,
        LANGUAGE_1,
        LANGUAGE_2
    }

    private List mList;
    private String username;
    // Practice options
    private AskedLanguage mAskedLanguage;
    private boolean mCaseSensitive = true;
    private boolean mIgnoreAccents = false;

    private final ArrayList<String[]> mWrongWords = new ArrayList<>();
    private String mRandomWord[] = new String[2];
    private int mTotalWords = 0;
    private final ArrayList<String[]> mWordsToGo = new ArrayList<>();

    private InputMethod mLastUsedPracticeMethod = InputMethod.KEYBOARD;

    private TableListViewAdapter mRecyclerViewAdapter;

    private SpeechRecognizer mSpeech = null;
    private Intent mRecognizerIntent;

    // UI elements
    private RecyclerView mRecyclerView;
    private EditText mTranslation;
    private TextView mRightWord;
    private Menu mMenu;
    private TextView mRightWordsCounter;
    private TextView mWrongWordsCounter;
    private TextView mWordsLeftCounter;
    private TextView mLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        // Setup Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        else throw new RuntimeException("getSupportActionBar() should not be null");

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
        View next = findViewById(R.id.next_word);
        if (next != null) {
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLastUsedPracticeMethod = InputMethod.KEYBOARD;
                    checkWord();
                }
            });
        } else throw new RuntimeException("next should not be null");

        /** Setup the {@link RecyclerView} */
        mRecyclerView = (RecyclerView) findViewById(R.id.wrong_words_list);
        /** Setup the {@link LinearLayoutManager} */
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        /** Setup the {@link TableListViewAdapter} */
        mRecyclerViewAdapter = new TableListViewAdapter(new ArrayList<String>(), new ArrayList<String>());
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        // Load other UI elements
        mRightWord = (TextView) findViewById(R.id.right_word);
        mRightWordsCounter = (TextView) findViewById(R.id.right_words_counter);
        mWrongWordsCounter = (TextView) findViewById(R.id.wrong_words_counter);
        mWordsLeftCounter = (TextView) findViewById(R.id.words_left_counter);
        mLanguage = (TextView) findViewById(R.id.language);

        // Load intent extras
        Intent intent = getIntent();
        mList = (List) intent.getSerializableExtra("list");
        username = intent.getStringExtra("username");
        mAskedLanguage = (AskedLanguage) intent.getSerializableExtra("askedLanguage");
        mCaseSensitive = intent.getBooleanExtra("caseSensitive", true);
        mIgnoreAccents = intent.getBooleanExtra("ignoreAccents", false);

        setupPractice();

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

                startSpeech();
                return true;
            case R.id.disable_speech:
                enable.setVisible(true);
                disable.setVisible(false);

                mSpeech.stopListening();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RECORD_AUDIO) {
            // Received permission result
            // Check if granted
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mSpeech.startListening(mRecognizerIntent);
            } else {
                // Permission not granted
                mMenu.findItem(R.id.enable_speech).setVisible(true);
                mMenu.findItem(R.id.disable_speech).setVisible(false);

                Snackbar.make(mRecyclerView, R.string.speech_permission_not_granted, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startSpeech() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Runtime Permissions
            if (checkSelfPermission(permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                // Permission is already available, start listening
                mSpeech.startListening(mRecognizerIntent);
            } else {
                // Microphone permission has not been granted
                if (shouldShowRequestPermissionRationale(permission.RECORD_AUDIO)) {
                    Toast.makeText(this, R.string.speech_permission_info, Toast.LENGTH_SHORT).show();
                }

                // Request permission
                requestPermissions(new String[]{permission.RECORD_AUDIO}, RECORD_AUDIO);
            }
        } else {
            // Permission already granted at install
            mSpeech.startListening(mRecognizerIntent);
        }
    }

    private void enableSpeech() {
        mSpeech = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeech.setRecognitionListener(this);
        mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        switch (mAskedLanguage) {
            case BOTH:
                if (!mList.getLanguage1().equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.getLanguage1()));
                if (!mList.getLanguage2().equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.getLanguage2()));
                break;

            case LANGUAGE_1:
                if (!mList.getLanguage1().equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.getLanguage1()));
                break;
            case LANGUAGE_2:
                if (!mList.getLanguage2().equals("lat"))
                    mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, List.getLocale(mList.getLanguage2()));
                break;
        }
    }

    private void setupPractice() {
        switch (mAskedLanguage) {
            case BOTH:
                // Add words to wordsToGo
                for (int i = 0; i < mList.getTotalWords(); i++) {
                    mWordsToGo.add(
                            new String[] {
                                    mList.getLanguage1Words().get(i),
                                    mList.getLanguage2Words().get(i)
                            }
                    );
                    mWordsToGo.add(
                            new String[] {
                                    mList.getLanguage2Words().get(i),
                                    mList.getLanguage1Words().get(i)
                            }
                    );
                }
                break;
            case LANGUAGE_1:
                // Display language
                if(mLanguage != null) mLanguage.setText(List.getLanguageName(this, mList.getLanguage1()));
                else throw new RuntimeException("mLanguage should not be null");
                // Add words to wordsToGo
                for (int i = 0; i < mList.getTotalWords(); i++) {
                    mWordsToGo.add(
                            new String[] {
                                    mList.getLanguage1Words().get(i),
                                    mList.getLanguage2Words().get(i)
                            }
                    );
                }
                break;
            case LANGUAGE_2:
                // Display language
                if(mLanguage != null) mLanguage.setText(List.getLanguageName(this, mList.getLanguage2()));
                else throw new RuntimeException("mLanguage should not be null");
                // Add words to wordsToGo
                for (int i = 0; i < mList.getTotalWords(); i++) {
                    mWordsToGo.add(
                            new String[] {
                                    mList.getLanguage2Words().get(i),
                                    mList.getLanguage1Words().get(i)
                            }
                    );
                }
                break;
        }
        setCounters();
        nextWord();
    }

    /**
     * Go to the next word
     */
    private void nextWord() {
        // Check if list is done
        if (mWordsToGo.size() == 0) {
            showPracticeResults();
            return;
        }

        int randomIndexInt = (int) Math.floor(Math.random() * mWordsToGo.size());
        mRandomWord = mWordsToGo.get(randomIndexInt);

        if (mAskedLanguage == AskedLanguage.BOTH && mLanguage != null) {
            // Display the right current asked language
            if (mList.getLanguage1Words().contains(mRandomWord[0])) {
                mLanguage.setText(ConvertLanguage.toLang(mList.getLanguage1()));
            } else mLanguage.setText(ConvertLanguage.toLang(mList.getLanguage2()));
        } else if (mLanguage == null) throw new RuntimeException("mLanguage should not be null");

        // Remove from wordsToGo
        mWordsToGo.remove(randomIndexInt);

        // Display
        TextView wordDisplay = (TextView) findViewById(R.id.word_to_translate);
        if (wordDisplay != null) wordDisplay.setText(mRandomWord[0]);
        else throw new RuntimeException("WordDisplay should not be null");
    }

    /**
     * Handle the word checking
     */
    private void checkWord() {
        mTotalWords++;
        if (isInputRight(mTranslation.getText().toString(), mRandomWord[1])) {
            mTranslation.setText("");
            mRightWord.setVisibility(View.GONE);
            setCounters();
            nextWord();
        } else {
            mWrongWords.add(new String[]{mRandomWord[1], mTranslation.getText().toString()});
            mRightWord.setText(mRandomWord[1]);
            mRightWord.setVisibility(View.VISIBLE);
            Snackbar.make(mTranslation, getString(R.string.error_wrong_translation), Snackbar.LENGTH_SHORT).show();

            // Add back to wordsToGo
            mWordsToGo.add(mRandomWord);

            setCounters();
        }

        if (mLastUsedPracticeMethod == InputMethod.SPEECH) {
            mSpeech.startListening(mRecognizerIntent);
        }
    }

    /**
     * Check if the inputted words are right
     * @param input Inputted word
     * @param correctWord Correct translation of the word
     * @return If the word is right
     */
    private boolean isInputRight(String input, String correctWord) {
        final String PERMISSIBLE_CHARACTERS = "\\(|\\{|\\[|\\]|\\}|\\)|\\s";

        // Remove accents if needed
        if (mIgnoreAccents) {
            input = unAccent(input);
            correctWord = unAccent(correctWord);
        }

        // Check for case sensitivity
        if (!mCaseSensitive || mLastUsedPracticeMethod == InputMethod.SPEECH) {
            input = input.toLowerCase();
            correctWord = correctWord.toLowerCase();
        }

        // Remove some specific characters
        input = input.replaceAll(PERMISSIBLE_CHARACTERS, "");
        correctWord = correctWord.replaceAll(PERMISSIBLE_CHARACTERS, "");

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

    /**
     * Function to remove accents from {@link String}
     * @param s string you want to normalize
     * @return {@link String} without accents
     */
    private String unAccent(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private void setCounters() {
        mWordsLeftCounter.setText(getString(R.string.current_words_left, mWordsToGo.size()));
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

        View practiceLayout = findViewById(R.id.practice_layout);
        View resultsLayout = findViewById(R.id.practice_results_layout);
        if (practiceLayout != null && resultsLayout != null) {
            practiceLayout.setVisibility(View.GONE);
            resultsLayout.setVisibility(View.VISIBLE);
        } else throw new RuntimeException("PracticeLayout or resultsLayout should not be null");

        // Set right percentages
        int rightPercentage = (int) Math.round(100 - ((double) mWrongWords.size() / (double) mTotalWords * 100));
        TextView rightText = (TextView) findViewById(R.id.right_text); 
        if (rightText != null) rightText.setText(getString(R.string.right_text, rightPercentage));
        else throw new RuntimeException("rightText should not be null");

        // Display the wrong words
        if (mWrongWords.size() > 0) {
            View wrongWordsLayout = findViewById(R.id.wrong_words_layout);
            if (wrongWordsLayout != null) wrongWordsLayout.setVisibility(View.VISIBLE);
            else throw new RuntimeException("wrongWordsLayout should not be null");

            mRecyclerViewAdapter.addItems(mWrongWords);
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

        if (matches != null && matches.size() > 0) {
            mTranslation.setText(matches.get(0));
            mLastUsedPracticeMethod = InputMethod.SPEECH;
            checkWord();
        }
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
