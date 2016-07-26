package edu.ksu.lti.launch.oauth;

import edu.ksu.lti.launch.spring.config.TestApplicationConfig;
import edu.ksu.lti.launch.spring.config.TestServiceConfig;
import edu.ksu.lti.launch.spring.config.TestSpringConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Created by alexanda on 7/26/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {TestSpringConfig.class, TestServiceConfig.class, TestApplicationConfig.class})
public class LtiLaunchITest {

    @Test
    public void shouldMakeRefreshToken() {
        
    }
}
