/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AuthPreferences {

    private static final String PREFS_NAME = "auth";
    private static final String KEY_ACCOUNT_NAME = "account_name";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_SELECTED_ACCOUNT = "selected_account";

    private final SharedPreferences preferences;

    public AuthPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getTotalAccounts() {
        return preferences.getStringSet(KEY_ACCOUNT_NAME, new HashSet<String>()).size();
    }

    public int getSelectedAccount() {
        return preferences.getInt(KEY_SELECTED_ACCOUNT, 0);
    }

    @Nullable
    public String getAccountName(int accountPosition) {
        Set<String> set = preferences.getStringSet(KEY_ACCOUNT_NAME, null);
        if (set != null) return set.toArray(new String[set.size()])[accountPosition];
        else return null;
    }

    @Nullable
    public String getAuthToken(int accountPosition) {
        Set<String> set = preferences.getStringSet(KEY_AUTH_TOKEN, null);
        if (set != null) return set.toArray(new String[set.size()])[accountPosition];
        else return null;
    }

    public void setSelectedAccount(int selectedAccount) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_ACCOUNT_NAME, selectedAccount);
        editor.apply();
    }

    public void setUsername(String accountName, int position) {
        Set<String> accounts = preferences.getStringSet(KEY_ACCOUNT_NAME, new HashSet<String>());
        ArrayList<String> accountsArray = new ArrayList<>(accounts);

        if (position < accountsArray.size()) {
            accountsArray.remove(position);
            accountsArray.add(position, accountName);
        } else {
            addUsername(accountName);
            return;
        }

        final SharedPreferences.Editor editor = preferences.edit();
        accounts = new HashSet<>();
        accounts.addAll(accountsArray);
        editor.putStringSet(KEY_ACCOUNT_NAME, accounts);
        editor.apply();
    }

    public void addUsername(String accountName) {
        Set<String> accounts = preferences.getStringSet(KEY_ACCOUNT_NAME, new HashSet<String>());
        accounts.add(accountName);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_ACCOUNT_NAME, accounts);
        editor.apply();
    }

    public void setAuthToken(String authToken, int position) {
        Set<String> tokens = preferences.getStringSet(KEY_AUTH_TOKEN, new HashSet<String>());
        ArrayList<String> tokensArray = new ArrayList<>(tokens);

        if (position < tokensArray.size()) {
            tokensArray.remove(position);
            tokensArray.add(position, authToken);
        } else {
            addAuthToken(authToken);
            return;
        }

        final SharedPreferences.Editor editor = preferences.edit();
        tokens = new HashSet<>();
        tokens.addAll(tokensArray);
        editor.putStringSet(KEY_AUTH_TOKEN, tokens);
        editor.apply();
    }

    public void addAuthToken(String authToken) {
        Set<String> tokens = preferences.getStringSet(KEY_AUTH_TOKEN, new HashSet<String>());
        tokens.add(authToken);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_AUTH_TOKEN, tokens);
        editor.apply();
    }

}
