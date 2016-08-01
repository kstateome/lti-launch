package edu.ksu.lti.launch.service;

/**
 * A service to be implemented and supplied as an autowire candidate by any consumers of this library
 * @author alexanda
 * @author ssnelson
 */
public interface OauthTokenService {

    /**
     * Fetch the refresh token from whatever persistence solution the consumer is currently using
     * @param userId The userId the refresh token needs to belong to
     * @return Refresh token to be used to get new API tokens, as a string
     */
    String getRefreshToken(String userId);

    /**
     * Stores a new refresh token in whatever persistence solution the consumer is using
     * @param userId The userId the refresh token belongs to
     * @param refreshToken The refresh token to be saved
     * @return Same refresh token passed in the refreshToken parameter
     */
    String storeToken(String userId, String refreshToken);

    /**
     * Updates the refresh token saved in the data source for a given user
     * Should only be called on very specific situations, however it is necessary
     * @param userId The userId the refresh token belongs to
     * @param refreshToken The refresh token to be saved
     * @return Same refresh token passed in the refreshToken parameter
     */
    String updateToken(String userId, String refreshToken);
}
