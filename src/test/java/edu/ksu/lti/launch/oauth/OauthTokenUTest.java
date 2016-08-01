package edu.ksu.lti.launch.oauth;

import edu.ksu.lti.launch.exception.RefreshFailedException;
import edu.ksu.lti.launch.service.OauthTokenRefreshService;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.internal.WhiteboxImpl;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OauthTokenUTest {

    private OauthToken testObject;

    // When using this mock, it's only public method is called 1 time during the setup method. Keep this in mind for tests
    @Mock
    private OauthTokenRefreshService mockRefreshService;

    private final String SOME_REFRESH_TOKEN = "someRefreshToken";
    private final String SOME_API_TOKEN = "someApiToken";


    @Before
    public void setup() throws IOException {
        when(mockRefreshService.getRefreshedOauthToken(SOME_REFRESH_TOKEN)).thenReturn(SOME_API_TOKEN);
        testObject = new OauthToken(SOME_REFRESH_TOKEN, mockRefreshService);
    }

    @Test
    public void shouldRefreshTokenOnObjectCreation() throws IOException {
        verify(mockRefreshService, times(1)).getRefreshedOauthToken(SOME_REFRESH_TOKEN);
    }

    @Test
    public void shouldNotBeExpiredOnObjectCreation() throws IOException {
        assertFalse("Api token should not be expired upon object creation", testObject.isExpired());
    }

    @Test
    public void shouldNotRefreshTokenWhenNotExpired() throws IOException {
        WhiteboxImpl.setInternalState(testObject, "lastUpdated", new Date());
        assertEquals("Api token should return the correct API token", SOME_API_TOKEN, testObject.getApiToken());
        verify(mockRefreshService, times(1)).getRefreshedOauthToken(SOME_REFRESH_TOKEN);
    }

    @Test
    public void shouldRefreshTokenWhenExpired() throws Exception {
        setUpExpiredDate();
        assertTrue("Api token should be expired when expired date", testObject.isExpired());

        String apiToken = testObject.getApiToken();

        assertEquals("Api token should return the correct API token", SOME_API_TOKEN, apiToken);
        verify(mockRefreshService, atLeastOnce()).getRefreshedOauthToken(SOME_REFRESH_TOKEN);
        assertFalse("Api token should not be expired after refreshed", testObject.isExpired());
    }

    @Test(expected = RefreshFailedException.class)
    public void shouldThrowExceptionWhenRefreshFails() throws IOException {
        setUpExpiredDate();
        when(mockRefreshService.getRefreshedOauthToken(SOME_REFRESH_TOKEN)).thenThrow(new IOException());
        testObject.getApiToken();
    }

    private void setUpExpiredDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -100);
        Date expiredDate = calendar.getTime();
        WhiteboxImpl.setInternalState(testObject, "lastUpdated", expiredDate);
    }
}