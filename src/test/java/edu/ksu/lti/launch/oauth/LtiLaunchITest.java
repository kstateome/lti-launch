package edu.ksu.lti.launch.oauth;

import edu.ksu.lti.launch.exception.NoLtiSessionException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.LtiSessionService;
import edu.ksu.lti.launch.service.OauthTokenRefreshService;
import edu.ksu.lti.launch.spring.config.TestApplicationConfig;
import edu.ksu.lti.launch.spring.config.TestServiceConfig;
import edu.ksu.lti.launch.spring.config.TestSpringConfig;
import edu.ksu.lti.launch.validator.OauthTokenValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by alexanda on 7/26/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {TestSpringConfig.class, TestServiceConfig.class, TestApplicationConfig.class})
public class LtiLaunchITest {

    @Autowired
    private OauthTokenValidator fakeOauthTokenValidator;
    @Autowired
    private LtiLaunch ltiLaunch;
    @Autowired
    private LtiSessionService fakeLtiSessionService;
    @Autowired
    private OauthTokenRefreshService fakeRefreshService;

    private final String VALID_TOKEN = "validToken";
    private final String INVALID_TOKEN = "invalidToken";
    private final String FAKE_EID = "fakeEid";

    @Before
    public void setup() throws IOException {
        when(fakeOauthTokenValidator.isValid(any())).thenReturn(false);
        when(fakeOauthTokenValidator.isValid(argThat(new ValidTokenMatcher()))).thenReturn(true);
        when(fakeRefreshService.getRefreshedOauthToken(any())).thenReturn(VALID_TOKEN);
    }

    @Test
    public void shouldCallRefreshTokenWhenTokenInvalid() throws NoLtiSessionException, IOException {
        LtiSession session = new LtiSession();
        session.setEid(FAKE_EID);
        session.setCanvasOauthToken(INVALID_TOKEN);
        when(fakeLtiSessionService.getLtiSession()).thenReturn(session);

        ltiLaunch.validateOAuthToken();

        verify(fakeRefreshService, atLeastOnce()).getRefreshedOauthToken(FAKE_EID);

    }


    class ValidTokenMatcher extends ArgumentMatcher<LtiSession> {

        @Override
        public boolean matches(Object argument) {
            return ((LtiSession) argument).getCanvasOauthToken().equals(VALID_TOKEN);
        }
    }

}
