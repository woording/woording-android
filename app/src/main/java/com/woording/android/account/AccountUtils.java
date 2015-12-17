package com.woording.android.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.Nullable;

public class AccountUtils {

    public static final String ACCOUNT_TYPE = "com.woording.android";
    public static final String AUTH_TOKEN_TYPE = "com.woording.android.auth";

    public static IServerAuthenticator mServerAuthenticator = new MyServerAuthenticator();

    @Nullable
    public static Account getAccount(Context context, String accountName) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equalsIgnoreCase(accountName)) {
                return account;
            }
        }
        return null;
    }

}
