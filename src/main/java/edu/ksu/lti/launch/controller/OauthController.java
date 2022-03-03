package edu.ksu.lti.launch.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.ksu.canvas.oauth.OauthTokenRefresher;
import edu.ksu.canvas.oauth.RefreshableOauthToken;
import edu.ksu.lti.launch.exception.CookieUnavailableException;
import edu.ksu.lti.launch.exception.NoLtiSessionException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.ConfigService;
import edu.ksu.lti.launch.service.LtiSessionService;
import edu.ksu.lti.launch.service.OauthTokenService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

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

    private final ConfigService configService;
    private final OauthTokenService oauthTokenService;
    private final LtiSessionService ltiSessionService;

    @Autowired
    private OauthController(ConfigService configService, OauthTokenService oauthTokenService, LtiSessionService ltiSessionService) {
        this.configService = configService;
        this.oauthTokenService = oauthTokenService;
        this.ltiSessionService = ltiSessionService;
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
        String oauthClientId = configService.getConfigValue("oauth_client_id");
        
        String randomUuid = UUID.randomUUID().toString();
        ltiSession.setOauthTokenRequestState(randomUuid);

        StringBuilder sb = new StringBuilder();
        sb.append("redirect:");
        sb.append("https://");
        sb.append(ltiSession.getCanvasDomain());
        sb.append("/login/oauth2/auth");
        sb.append("?");
        sb.append("client_id=");
        sb.append(oauthClientId);
        sb.append("&state=");
        sb.append(randomUuid);
        sb.append("&response_type=code");
        sb.append("&redirect_uri=");
        sb.append(getApplicationBaseUrl(request, true));
        sb.append("/oauthResponse");
        LOG.debug("returning from start oauth: " + sb.toString());
        return sb.toString();
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
        
        String canvasUrl = configService.getConfigValue("canvas_url");
        String oauthClientId = configService.getConfigValue("oauth_client_id");
        String oauthClientSecret = configService.getConfigValue("oauth_client_secret");
        if(oauthCode != null && !oauthCode.trim().isEmpty()) {
            try {
                LOG.debug("got oauth code back: " + oauthCode);
                URL tokenUrl = new URL(canvasUrl + "/login/oauth2/token");
                HttpURLConnection con = (HttpURLConnection)tokenUrl.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                OutputStream out = con.getOutputStream();
                StringBuilder paramsBuilder = new StringBuilder();
                paramsBuilder.append("client_id=");
                paramsBuilder.append(oauthClientId);
                paramsBuilder.append("&client_secret=");
                paramsBuilder.append(oauthClientSecret);
                paramsBuilder.append("&code=");
                paramsBuilder.append(oauthCode);
                paramsBuilder.append("&redirect_uri=");
                paramsBuilder.append(getApplicationBaseUrl(request, true));
                paramsBuilder.append("/oauthResponse");
                LOG.debug("sending params to get oauth token: " + paramsBuilder.toString());
                out.write(paramsBuilder.toString().getBytes());
                out.flush();
                out.close();

                int responseCode = con.getResponseCode();
                LOG.debug("got response code from token request: " + responseCode);
                LOG.debug("response message: " + con.getResponseMessage());

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                LOG.debug("content: " + content.toString());
                JsonObject jobj = new Gson().fromJson(content.toString(), JsonObject.class);
                String accessToken = jobj.get("access_token").getAsString();
                String refreshToken = jobj.get("refresh_token").getAsString();
                String eID = ltiSession.getEid();
                LOG.debug("access token for eid " + eID + ": " + accessToken);
                LOG.debug("refresh token for eid " + eID + ": " + refreshToken);
                
                String token = oauthTokenService.getRefreshToken(eID);
                if (token == null) {
                    oauthTokenService.storeToken(eID, refreshToken);
                } else {
                    oauthTokenService.updateToken(eID, refreshToken);
                }
                OauthTokenRefresher tokenRefresher = new OauthTokenRefresher(oauthClientId, oauthClientSecret, canvasUrl);
                ltiSession.setOauthToken(new RefreshableOauthToken(tokenRefresher, refreshToken));

            }
            catch(IOException e) {
                LOG.error("error getting oauth token", e);
            }
        }
        return "redirect:" + ltiSession.getInitialViewPath();
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
