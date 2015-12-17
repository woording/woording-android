package com.woording.android.account;

public interface IServerAuthenticator {

    /**
     * Tells the server to create the new user and return its auth token.
     * @param email
     * @param username
     * @param password
     * @return Access token
     */
    public String signUp (final String email, final String username, final String password);

    /**
     * Logs the user in and returns its auth token.
     * @param email
     * @param password
     * @return Access token
     */
    public String signIn (final String email, final String password);

}
