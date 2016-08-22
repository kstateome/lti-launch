package edu.ksu.lti.launch.test;


import edu.ksu.lti.launch.controller.OauthController;
import edu.ksu.lti.launch.oauth.LtiLaunch;
import edu.ksu.lti.launch.service.OauthTokenRefreshService;
import edu.ksu.lti.launch.service.OauthTokenService;
import edu.ksu.lti.launch.spring.config.TestApplicationConfig;
import edu.ksu.lti.launch.spring.config.TestSpringConfig;
import edu.ksu.lti.launch.spring.config.TestServiceConfig;
import edu.ksu.lti.launch.controller.TestLtiLaunchController;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertNotNull;

/*
 * This test simply tests that the spring context is able to be setup in an appropiate way.
 * It does this just by using a spring context similar to one that client code will need
 * to set up.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {TestSpringConfig.class, TestApplicationConfig.class})
public class SpringContextITest {
    @Autowired
    private TestLtiLaunchController testLtiLaunchController;
    @Autowired
    private OauthController oauthController;
    @Autowired
    private LtiLaunch ltiLaunch;
    @Autowired
    private OauthTokenRefreshService oauthTokenRefreshService;
    @Autowired
    private OauthTokenService oauthTokenService;

    @Test
    public void testSpringContext() {
        //The test will fail if the spring context is not setup appropriately.
        assertNotNull("Expected testLtiLaunchController to be instantiated by Spring", testLtiLaunchController);
        assertNotNull("Expected oauthController to be instantiated by Spring", oauthController);
        assertNotNull("Expected ltiLaunchCanvas to be instantiated by Spring", ltiLaunch);
        assertNotNull("Expected oauthTokenRefreshService to be instantiated by Spring", oauthTokenRefreshService);
        assertNotNull("Expected oauthTokenService to be instantiated by Spring", oauthTokenService);
    }

}
