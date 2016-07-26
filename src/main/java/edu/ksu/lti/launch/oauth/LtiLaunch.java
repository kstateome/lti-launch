package edu.ksu.lti.launch.oauth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.ksu.lti.launch.exception.NoLtiSessionException;
import edu.ksu.lti.launch.exception.OauthTokenRequiredException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.LtiLaunchKeyService;
import edu.ksu.lti.launch.service.OauthTokenService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

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
    private LtiLaunchKeyService launchKeyService;
    @Autowired
    private String canvasDomain;

    /**
     * Get the LtiSession object from the HTTP session. It is put there up in the ltiLaunch method.
     * This should really be done using a SpringSecurityContext to get the authenticated principal
     * which could then hold this LTI information. But I'm having serious trouble figuring out how to
     * do this correctly and I need *some* kind of session management for right now.
     * Another approach would be to create this as a session scoped bean but the problem there is that
     * I need to share this session object across controllers (the OauthController to be specific) and
     * this breaks for some reason so I'm rolling my own session management here.
     *
     * @return The current user's LTI session information
     * @throws NoLtiSessionException if the user does not have a valid LTI session.
     */
    public LtiSession getLtiSession() throws NoLtiSessionException {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = sra.getRequest();
        HttpSession session = req.getSession();
        LtiSession ltiSession = (LtiSession) session.getAttribute(LtiSession.class.getName());
        if (ltiSession == null) {
            throw new NoLtiSessionException();
        }
        return ltiSession;
    }

    public String ensureApiTokenPresent() throws OauthTokenRequiredException, NoLtiSessionException {
        LtiSession ltiSession = getLtiSession();
        if (ltiSession.getCanvasOauthToken() != null) {
            return ltiSession.getCanvasOauthToken();
        }

        String eid = ltiSession.getEid();
        String token = oauthTokenService.getOauthToken(eid);
        if (StringUtils.isBlank(token)) {
            LOG.info("no API key for user. Sending to oauth flow: " + eid);
            throw new OauthTokenRequiredException();
        }
        ltiSession.setCanvasOauthToken(token);
        return token;
    }

    public void refreshOauthToken() throws NoLtiSessionException, IOException {
        LOG.debug("Refreshing token.");
        LtiSession ltiSession = getLtiSession();
        HttpPost canvasRequest = createRefreshCanvasRequest(ltiSession);
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(canvasRequest);
        if (response.getStatusLine() == null || response.getStatusLine().getStatusCode() == 401) {
            LOG.warn("Refresh failed. Redirect to oauth flow");
            throw new OauthTokenRequiredException();
        } else {
            JsonObject responseContent = new Gson().fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
            String accessToken = responseContent.get("access_token").getAsString();
            LOG.debug("Refreshed access token for eid " + ltiSession.getEid() + ": " + accessToken);
            oauthTokenService.updateToken(ltiSession.getEid(), accessToken);
            ltiSession.setCanvasOauthToken(accessToken);
        }
        validateOAuthToken();
    }

    /**
     * Perform a trivial Canvas operation to verify if the OAuth Token is valid.
     * This will typically be used when we want an integration to fail fast.
     * Some integrations might not notice that the OAuth token is invalid until
     * halfway into whatever it is that they do. This was initially the case
     * for the Scantron integration.
     *
     * @throws NoLtiSessionException       When there isn't a valid ltiExcpetion
     * @throws OauthTokenRequiredException when the oauthtoken isn't valid
     * @throws IOException                 when exception communicating with canvas
     */
    public void validateOAuthToken() throws NoLtiSessionException, OauthTokenRequiredException, IOException {
        LtiSession ltiSession = getLtiSession();
        HttpGet canvasRequest = createCanvasRequest(ltiSession);
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(canvasRequest);
        if (response.getStatusLine() == null || response.getStatusLine().getStatusCode() == 401) {
            if (oauthTokenService.getRefreshToken(ltiSession.getEid()) == null) {
                throw new OauthTokenRequiredException();
            }
            refreshOauthToken();
        }
    }

    private HttpGet createCanvasRequest(LtiSession ltiSession) {
        try {
            URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost(canvasDomain)
                    .setPath("/api/v1/users/self/todo")
                    .build();
            HttpGet canvasRequest = new HttpGet(uri);
            canvasRequest.addHeader("Authorization", "Bearer " + ltiSession.getCanvasOauthToken());
            return canvasRequest;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid uri for canvas when validating oauthToken", e);
        }
    }

    private HttpPost createRefreshCanvasRequest(LtiSession ltiSession) {
        try {
            URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost(canvasDomain)
                    .setPath("/login/oauth2/token")
                    .build();
            HttpPost canvasRequest = new HttpPost(uri);
            List<NameValuePair> paramList = new LinkedList<>();
            paramList.add(new BasicNameValuePair("grant_type", "refresh_token"));
            paramList.add(new BasicNameValuePair("client_id", launchKeyService.findOauthClientId()));
            paramList.add(new BasicNameValuePair("client_secret", launchKeyService.findOauthClientSecret()));
            paramList.add(new BasicNameValuePair("refresh_token", oauthTokenService.getRefreshToken(ltiSession.getEid())));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);

            canvasRequest.setEntity(entity);
            return canvasRequest;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid uri for canvas when requesting refresh oauthToken", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Invalid encoding for canvas when requesting refresh oauthToken", e);
        }
    }
}
