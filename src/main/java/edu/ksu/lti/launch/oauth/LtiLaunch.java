package edu.ksu.lti.launch.oauth;

import edu.ksu.lti.launch.exception.NoLtiSessionException;
import edu.ksu.lti.launch.exception.OauthTokenRequiredException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.LtiSessionService;
import edu.ksu.lti.launch.service.OauthTokenRefreshService;
import edu.ksu.lti.launch.service.OauthTokenService;
import edu.ksu.lti.launch.validator.OauthTokenValidator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * This class was extracted from a subset of functions from LtiLaunchController
 */
@Component
@Scope("session")
public class LtiLaunch {
    private static final Logger LOG = Logger.getLogger(LtiLaunch.class);
    @Autowired
    private OauthTokenService oauthTokenService;
    @Autowired
    private OauthTokenRefreshService oauthTokenRefreshService;
    @Autowired
    private OauthTokenValidator oauthTokenValidator;
    @Autowired
    private LtiSessionService ltiSessionService;



    public String ensureApiTokenPresent() throws OauthTokenRequiredException, NoLtiSessionException {
        LtiSession ltiSession = ltiSessionService.getLtiSession();
        if (ltiSession.getOauthToken() != null) {
            return ltiSession.getApiToken();
        }
        if (ltiSession.getEid() != null) {
            String refreshToken = oauthTokenService.getRefreshToken(ltiSession.getEid());
            if (refreshToken != null) {
                ltiSession.setOauthToken(new OauthToken(refreshToken, oauthTokenRefreshService));
                return ltiSession.getApiToken();
            }
            throw new OauthTokenRequiredException();
        }
        // If the eid is null, we need to get a new session.
        throw new NoLtiSessionException();

    }

    /**
     * Perform a trivial Canvas operation to verify if the OAuth Token is valid.
     * This will typically be used when we want an integration to fail fast.
     * Some integrations might not notice that the OAuth token is invalid until
     * halfway into whatever it is that they do. This was initially the case
     * for the Scantron integration.
     *
     * @throws NoLtiSessionException       When there isn't a valid ltiExcpetion
     * @throws IOException                 when exception communicating with canvas
     */
    public void validateOAuthToken() throws NoLtiSessionException, IOException {
        LtiSession ltiSession = ltiSessionService.getLtiSession();
        if (!oauthTokenValidator.isValid(ltiSession)) {
            throw new OauthTokenRequiredException();
        }
    }



}
