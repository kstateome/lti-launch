package edu.ksu.lti.launch.service;

import edu.ksu.lti.launch.exception.OauthTokenRequiredException;
import edu.ksu.lti.launch.util.CanvasResponseParser;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OauthTokenRefreshServiceUTest {

    private static final String SOME_CANVAS_DOMAIN = "someCanvasDomain";
    private static final String USER_EID = "someEid";
    private static final String OAUTH_TOKEN_URL = "https://" + SOME_CANVAS_DOMAIN + "/login/oauth2/token";
    private static final String TOKEN = "token";

    @Mock
    private LtiLaunchKeyService mockLtiLaunchKeyService;
    @Mock
    private OauthTokenService mockOauthTokenService;
    @Mock
    private HttpClientBuilder mockHttpClientBuilder;
    @Mock
    private CloseableHttpResponse mockHttpResponse;
    @Mock
    private CanvasResponseParser canvasResponseParser;
    @InjectMocks
    private OauthTokenRefreshService oauthTokenRefreshService;
    @Mock
    private CloseableHttpClient mockHttpClient;

    @Before
    public void setup() {
        Whitebox.setInternalState(oauthTokenRefreshService, SOME_CANVAS_DOMAIN);
    }

    @Before
    public void setupMocks() throws Exception {
        when(mockHttpClientBuilder.build()).thenReturn(mockHttpClient);
        when(mockHttpClient.execute(any())).thenReturn(mockHttpResponse);
    }

    @Test
    public void correctTokenReturnedWhenResponseIsOk() throws Exception {
        setupOkResponse();

        String returnedToken = oauthTokenRefreshService.getRefreshedOauthToken(USER_EID);

        Assert.assertEquals("Expected correct token to be returned by getRefreshedOauthToken", TOKEN, returnedToken);
    }

    @Test
    public void oauthTokenUrlIsCorrectlyBuilt() throws Exception {
        ArgumentCaptor<HttpPost> httpPostArgumentCaptor = ArgumentCaptor.forClass(HttpPost.class);
        setupOkResponse();

        oauthTokenRefreshService.getRefreshedOauthToken(USER_EID);

        verify(mockHttpClient).execute(httpPostArgumentCaptor.capture());
        URI oauthTokenUri = httpPostArgumentCaptor.getValue().getURI();
        Assert.assertEquals("Oauth token url is incorrect", OAUTH_TOKEN_URL, oauthTokenUri.toString());
    }

    @Test
    public void keysAreRetrievedWhenBuildingResponse() throws Exception {
        setupOkResponse();

        oauthTokenRefreshService.getRefreshedOauthToken(USER_EID);

        verify(mockLtiLaunchKeyService).findOauthClientId();
        verify(mockLtiLaunchKeyService).findOauthClientSecret();
    }

    @Test(expected = OauthTokenRequiredException.class)
    public void oauthTokenRequiredThrownWhenStatusIsNull() throws Exception {
        when(mockHttpResponse.getStatusLine()).thenReturn(null);

        oauthTokenRefreshService.getRefreshedOauthToken(USER_EID);
    }

    @Test(expected = OauthTokenRequiredException.class)
    public void oauthTokenRequiredExceptionThrownWhenStatusIsUnauthorized() throws Exception {
        StatusLine statusLine = StatusLines.UNAUTHORIZED;
        when(mockHttpResponse.getStatusLine()).thenReturn(statusLine);

        oauthTokenRefreshService.getRefreshedOauthToken(USER_EID);
    }

    @Test(expected = IOException.class)
    public void exceptionThrownWhenResponseIsNotOK() throws Exception {
        StatusLine statusLine = StatusLines.INTERNAL_ERROR;
        when(mockHttpResponse.getStatusLine()).thenReturn(statusLine);

        oauthTokenRefreshService.getRefreshedOauthToken(USER_EID);
    }



    private void setupOkResponse() throws Exception {
        StatusLine statusLine = StatusLines.OK;
        when(mockHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(canvasResponseParser.parseToken(mockHttpResponse)).thenReturn(TOKEN);
    }

    private static class StatusLines {
        private static ProtocolVersion SOME_PROTOCOL_VERSION = new ProtocolVersion("someProtocolVersion", 1, 1);
        private static final String SOME_REASON = "someReason";
        private static final StatusLine UNAUTHORIZED = unauthorizedStatusLine();
        private static final StatusLine INTERNAL_ERROR = internalErrorStatusLine();
        private static final StatusLine OK = okStatusLine();


        private static StatusLine unauthorizedStatusLine() {
            int unauthorizedStatus = 401;
            return new BasicStatusLine(SOME_PROTOCOL_VERSION, unauthorizedStatus, SOME_REASON);
        }

        private static StatusLine internalErrorStatusLine() {
            int unauthorizedStatus = 500;
            return new BasicStatusLine(SOME_PROTOCOL_VERSION, unauthorizedStatus, SOME_REASON);
        }

        private static StatusLine okStatusLine() {
            int okStatus = 200;
            return new BasicStatusLine(SOME_PROTOCOL_VERSION, okStatus, SOME_REASON);
        }
    }


}
