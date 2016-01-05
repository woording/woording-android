/*
 * Woording for Android is a project by PhiliPdB.
 *
 * Copyright (c) 2016.
 */

package com.woording.android.account;

public interface IServerAuthenticator {

    /**
     * Tells the server to create the new user and return its auth token.
     * @param email user's email
     * @param username user's username
     * @param password user's password
     * @return Access token
     */
    String signUp (final String email, final String username, final String password);

    /**
     * Logs the user in and returns its auth token.
     * @param username user's username
     * @param password user's password
     * @return Access token
     */
    String signIn (final String username, final String password);

}
