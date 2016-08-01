package edu.ksu.lti.launch.oauth;

import edu.ksu.lti.launch.exception.RefreshFailedException;
import edu.ksu.lti.launch.service.OauthTokenRefreshService;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author alexanda
 * @author ssnelson
 */
public class OauthToken {

    private static Logger LOG = Logger.getLogger(OauthToken.class);

    private OauthTokenRefreshService refreshService;

    private String refreshToken;
    private String apiToken;
    private Date lastUpdated;

    /**
     * Since OAuth tokens currently expire every hour, this is initialized to expire the tokens and refresh them every 45 minutes
     * This gives us a buffer around the time in which it will expire, and should help prevent users from accidentally getting an expired token
     */
    private static final long TIMEOUT = MILLISECONDS.convert(45, MINUTES);

    public OauthToken(String refreshToken, OauthTokenRefreshService refreshService) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token may not be null or empty.");
        }
        this.refreshToken = refreshToken;
        if (refreshService == null) {
            throw new IllegalArgumentException("Refresh service may not be null");
        }
        this.refreshService = refreshService;
        init();
    }

    /**
     * Gets, and if necessary, refreshes, the API Token needed for making requests
     * @return the API token
     */
    public String getApiToken() {
        if (isExpired()) {
            refresh();
        }
        return apiToken;
    }

    /**
     * Initialize this object by using the refresh token to get a new api token and set it
     */
    private void init() {
        refresh();
    }

    /**
     * Checks whether this token is expired
     * @return true if the token is older than the timeout, else false
     */
    private boolean isExpired() {
        return (new Date().getTime() - lastUpdated.getTime()) >= TIMEOUT;

    }

    /**
     * Refreshes the apiToken contained in this object
     */
    private void refresh() {
        LOG.debug("Refreshing oauth token");
        try {
            this.apiToken = refreshService.getRefreshedOauthToken(refreshToken);
        } catch (IOException e) {
            // The chances of this happening are rather slim, so only basic handling for now
            LOG.error("Unable to refresh token with IOException: " + e.getMessage());
            throw new RefreshFailedException("Unable to refresh token", e);
        }
        this.lastUpdated = new Date();
    }
}
