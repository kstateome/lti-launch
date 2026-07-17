package edu.ksu.lti.launch.oauth;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.ksu.lti.launch.service.LtiLaunchKeyService;

/**
 * Legacy helper retained for compatibility with existing wiring. The Spring Security OAuth
 * ConsumerDetails types were removed during the Jakarta migration, so this now only
 * performs consumer key to shared secret lookup.
 */
@Component
public class LtiConsumerDetailsService {
    private static final Logger LOG = LogManager.getLogger(LtiConsumerDetailsService.class);

    private final LtiLaunchKeyService ltiKeyService;

    @Autowired
    public LtiConsumerDetailsService(LtiLaunchKeyService ltiKeyService) {
        this.ltiKeyService = ltiKeyService;
    }

    public String findSecretForConsumerKey(String consumerKey) {
        if(StringUtils.isBlank(consumerKey)) {
            throw new IllegalArgumentException("Supplied LTI key can not be blank");
        }
        LOG.debug("Looking up shared secret for LTI key {}", consumerKey);
        return ltiKeyService.findSecretForKey(consumerKey);
    }
}
