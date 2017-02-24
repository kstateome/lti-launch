package edu.ksu.lti.launch.exception;

/**
 * To be thrown if we do not have an OAuth token for the current user.
 * 
 * Typically the user will be redirected to the OAuth flow to grant
 * the application access to their account.
 *
 */
public class OauthTokenRequiredException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
