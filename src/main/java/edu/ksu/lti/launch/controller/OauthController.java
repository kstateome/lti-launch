package edu.ksu.lti.launch.controller;

import edu.ksu.canvas.oauth.OauthTokenRefresher;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.oauth.RefreshableOauthToken;
import edu.ksu.lti.launch.exception.CookieUnavailableException;
import edu.ksu.lti.launch.exception.NoLtiSessionException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.ConfigService;
import edu.ksu.lti.launch.service.LtiSessionService;
import edu.ksu.lti.launch.service.OauthTokenService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.Set;

/**
 * Controller for obtaining an oauth token for a user. It redirects the 
 * user to the Canvas oauth endpoint which requests the user's permission.
 * Once they grant permission, a request comes back here with a "code" which 
 * is then used to make another request from the server to Canvas to verify
 * things and finally get the actual token back.
 */
@Controller
public class OauthController {
    private static final Logger LOG = LogManager.getLogger(OauthController.class);
    private static final String CANVAS_REGISTRATION_ID = "canvas";

    private final ConfigService configService;
    private final OauthTokenService oauthTokenService;
    private final LtiSessionService ltiSessionService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> oauth2AccessTokenResponseClient;

    @Autowired
    OauthController(ConfigService configService,
                    OauthTokenService oauthTokenService,
                    LtiSessionService ltiSessionService,
                    ClientRegistrationRepository clientRegistrationRepository,
                    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> oauth2AccessTokenResponseClient) {
        this.configService = configService;
        this.oauthTokenService = oauthTokenService;
        this.ltiSessionService = ltiSessionService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oauth2AccessTokenResponseClient = oauth2AccessTokenResponseClient;
    }

    @RequestMapping("/beginOauth")
    public String startOauth(HttpServletRequest request) throws NoLtiSessionException, CookieUnavailableException {
        LtiSession ltiSession;
        try {
            ltiSession = ltiSessionService.getLtiSession();
        } catch (NoLtiSessionException cookieIssue) {
            LOG.trace(cookieIssue); // just here to shut sonar up.
            LOG.warn("Could not get the newly created lti session, this indicates a browser is not accepting our cookies.");
            throw new CookieUnavailableException("Failed to retrieve new LTI Session from cookie. User must change their cookie settings.");
        }
        LOG.debug("Sending user " + ltiSession.getEid() + " to get oauth token at " + ltiSession.getCanvasDomain());
        ClientRegistration clientRegistration = getCanvasClientRegistration();
        
        String randomUuid = UUID.randomUUID().toString();
        ltiSession.setOauthTokenRequestState(randomUuid);

        String redirectUri = getApplicationBaseUrl(request, true) + "/oauthResponse";
        OAuth2AuthorizationRequest.Builder authorizationBuilder = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
            .clientId(clientRegistration.getClientId())
            .redirectUri(redirectUri)
            .state(randomUuid);
        Set<String> scopes = clientRegistration.getScopes();
        if (scopes != null && !scopes.isEmpty()) {
            authorizationBuilder.scopes(scopes);
        }

        String authorizationRequestUri = authorizationBuilder.build().getAuthorizationRequestUri();
        LOG.debug("returning from start oauth: redirect:" + authorizationRequestUri);
        return "redirect:" + authorizationRequestUri;
    }

    @RequestMapping("/oauthResponse")
    public String oauthResponse(
    		HttpServletRequest request, 
    		@ModelAttribute(value="code") String oauthCode, 
    		@ModelAttribute(value="state") String state, 
    		@ModelAttribute(value="error") String errorMsg) throws NoLtiSessionException {
    	
        LtiSession ltiSession = ltiSessionService.getLtiSession();
        LOG.info("got oauth token for " + ltiSession.getEid());
        LOG.debug("got oauth response: " + oauthCode);
        LOG.debug("got oauth state: "+state);
        LOG.debug("oauth error: " + errorMsg);
        
        if(!ltiSession.getOauthTokenRequestState().equals(state)) {
        	String msg = "In the OAuth Token Response, the state does not match what we sent! " +
        			     "A Cross Site Script Forgery Request may be in progress. Aborting process!";
        	throw new RuntimeException(msg);
        }
        
        if(StringUtils.isNotBlank(oauthCode)) {
            try {
                LOG.debug("got oauth code back: " + oauthCode);
                ClientRegistration clientRegistration = getCanvasClientRegistration();
                String redirectUri = getApplicationBaseUrl(request, true) + "/oauthResponse";
                OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                    .clientId(clientRegistration.getClientId())
                    .redirectUri(redirectUri)
                    .state(state)
                    .build();
                OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(oauthCode)
                    .redirectUri(redirectUri)
                    .state(state)
                    .build();
                OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(
                    clientRegistration,
                    new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
                OAuth2AccessTokenResponse tokenResponse = oauth2AccessTokenResponseClient.getTokenResponse(grantRequest);
                String accessToken = tokenResponse.getAccessToken().getTokenValue();
                String refreshToken = tokenResponse.getRefreshToken() != null ? tokenResponse.getRefreshToken().getTokenValue() : null;
                String eID = ltiSession.getEid();
                LOG.debug("access token for eid " + eID + ": " + accessToken);
                LOG.debug("refresh token for eid " + eID + ": " + refreshToken);

                if (StringUtils.isNotBlank(refreshToken)) {
                    String canvasUrl = configService.getConfigValue("canvas_url");
                    String token = oauthTokenService.getRefreshToken(eID);
                    if (token == null) {
                        oauthTokenService.storeToken(eID, refreshToken);
                    } else {
                        oauthTokenService.updateToken(eID, refreshToken);
                    }

                    // Keep existing canvas-api integration behavior by wrapping refresh token.
                    ltiSession.setOauthToken(createRefreshableOauthToken(
                        clientRegistration.getClientId(),
                        clientRegistration.getClientSecret(),
                        canvasUrl,
                        refreshToken));
                } else {
                    LOG.warn("No refresh_token returned by Canvas for user {}", eID);
                }
            }
            catch(OAuth2AuthorizationException e) {
                LOG.error("error getting oauth token", e);
            }
            catch(RuntimeException e) {
                LOG.error("error getting oauth token", e);
            }
        }
        return "redirect:" + ltiSession.getInitialViewPath();
    }

    private ClientRegistration getCanvasClientRegistration() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(CANVAS_REGISTRATION_ID);
        if (clientRegistration == null) {
            throw new RuntimeException("Canvas OAuth2 client registration is not configured");
        }
        return clientRegistration;
    }

    protected OauthToken createRefreshableOauthToken(String clientId, String clientSecret, String canvasUrl,
                                                     String refreshToken) {
        return new RefreshableOauthToken(new OauthTokenRefresher(clientId, clientSecret, canvasUrl), refreshToken);
    }

    /** Returns the base URL of this application. This includes scheme, hostname
     * and port number. Useful for when you need an absolute URL pointing to
     * something in this application but don't want to hard-code so it still
     * works in alpha, beta and production. Does NOT include a trailing slash.
     * @param request servlet request
     * @param includeLtiApp Specify whether to include the application context part of the URL (/bioDemo, /learnItLive, etc)
     * @return Base URL of this application
     */
    public static String getApplicationBaseUrl(HttpServletRequest request, boolean includeLtiApp) {
        StringBuffer sb = new StringBuffer();
        sb.append(request.getScheme());
        sb.append("://");
        sb.append(request.getServerName());

        if(request.getServerPort() != 80 && request.getServerPort() != 443) {
            sb.append(":");
            sb.append(request.getServerPort());
        }
        if(includeLtiApp) {
            LOG.debug("context path: " + request.getContextPath());
            sb.append(request.getContextPath());
        }
        return sb.toString();
    }
}
