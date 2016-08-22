package edu.ksu.lti.launch.service;

import edu.ksu.lti.launch.exception.OauthTokenRequiredException;
import edu.ksu.lti.launch.util.CanvasResponseParser;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

@Service
public class OauthTokenRefreshService {

    private static Logger LOG = Logger.getLogger(OauthTokenRefreshService.class);

    @Autowired
    private LtiLaunchKeyService launchKeyService;
    @Autowired
    private String canvasDomain;
    @Autowired
    private CanvasResponseParser canvasResponseParser;

    public String getRefreshedOauthToken(String refreshToken) throws IOException {
        HttpPost canvasRequest = createRefreshCanvasRequest(refreshToken);
        try (CloseableHttpClient httpClient = createHttpClient();
             CloseableHttpResponse response = httpClient.execute(canvasRequest)) {
            if (response.getStatusLine() == null
                    || response.getStatusLine().getStatusCode() == 401
                    || response.getStatusLine().getStatusCode() == 400) {
                LOG.warn("Refresh failed. Redirect to oauth flow");
                throw new OauthTokenRequiredException();
            } else if (response.getStatusLine().getStatusCode() != 200) {
                String tokenUri = canvasRequest.getURI().toString();
                throw new IOException(tokenUri + " returned a status of " + response.getStatusLine().getStatusCode());
            } else {
                return canvasResponseParser.parseToken(response);
            }
        }
    }

    private HttpPost createRefreshCanvasRequest(String refreshToken) {
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
            paramList.add(new BasicNameValuePair("refresh_token", refreshToken));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);

            canvasRequest.setEntity(entity);
            return canvasRequest;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid uri for canvas when requesting refresh oauthToken", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Invalid encoding for canvas when requesting refresh oauthToken", e);
        }
    }

    protected CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create().build();
    }

}
