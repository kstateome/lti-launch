package edu.ksu.lti.launch.oauth;

import edu.ksu.lti.launch.exception.NoLtiSessionException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.LtiSessionService;
import edu.ksu.lti.launch.service.OauthTokenRefreshService;
import edu.ksu.lti.launch.validator.OauthTokenValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LtiLaunchUTest {

    @Mock
    private OauthTokenValidator mockOauthTokenValidator;
    @Mock
    private LtiSessionService mockLtiSessionService;
    @Mock
    private OauthTokenRefreshService mockRefreshService;
    @InjectMocks
    private LtiLaunch ltiLaunch;

    private final String VALID_TOKEN = "validToken";
    private final String INVALID_TOKEN = "invalidToken";
    private final String FAKE_EID = "fakeEid";

    @Before
    public void setupMocks() throws IOException {
        reset(mockOauthTokenValidator, mockLtiSessionService, mockRefreshService);
        when(mockOauthTokenValidator.isValid(any())).thenReturn(false);
        when(mockOauthTokenValidator.isValid(argThat(new ValidTokenMatcher()))).thenReturn(true);
        when(mockRefreshService.getRefreshedOauthToken(any())).thenReturn(VALID_TOKEN);
    }

    @Test
    public void shouldCallRefreshTokenWhenTokenInvalid() throws NoLtiSessionException, IOException {
        LtiSession session = new LtiSession();
        session.setEid(FAKE_EID);
        session.setCanvasOauthToken(INVALID_TOKEN);
        when(mockLtiSessionService.getLtiSession()).thenReturn(session);

        ltiLaunch.validateOAuthToken();

        verify(mockRefreshService, atLeastOnce()).getRefreshedOauthToken(FAKE_EID);

    }

    @Test
    public void shouldNotCallRefreshTokenWhenTokenValid() throws NoLtiSessionException, IOException {
        LtiSession session = new LtiSession();
        session.setEid(FAKE_EID);
        session.setCanvasOauthToken(VALID_TOKEN);
        when(mockLtiSessionService.getLtiSession()).thenReturn(session);

        ltiLaunch.validateOAuthToken();

        verify(mockRefreshService, times(0)).getRefreshedOauthToken(any());
    }


    class ValidTokenMatcher extends ArgumentMatcher<LtiSession> {

        @Override
        public boolean matches(Object argument) {
            return ((LtiSession) argument).getCanvasOauthToken().equals(VALID_TOKEN);
        }
    }
}
