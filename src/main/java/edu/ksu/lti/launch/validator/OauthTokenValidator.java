package edu.ksu.lti.launch.validator;

import edu.ksu.lti.launch.exception.OauthTokenRequiredException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.OauthTokenService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by alexanda on 7/26/16.
 */
@Component
public class OauthTokenValidator {

    @Autowired
    private OauthTokenService oauthTokenService;
    @Autowired
    private String canvasDomain;

    public boolean isValid(LtiSession ltiSession) throws IOException, OauthTokenRequiredException {
        HttpGet canvasRequest = createCanvasRequest(ltiSession);
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(canvasRequest);
        if (response.getStatusLine() == null || response.getStatusLine().getStatusCode() == 401) {
            if (oauthTokenService.getRefreshToken(ltiSession.getEid()) == null) {
                throw new OauthTokenRequiredException();
            }
            return false;
        }
        return true;
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
}
