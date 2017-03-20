package edu.ksu.lti.launch.spring.config;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.lti.launch.oauth.LtiLaunch;
import edu.ksu.lti.launch.service.ConfigService;
import edu.ksu.lti.launch.service.LtiLaunchKeyService;
import edu.ksu.lti.launch.service.OauthTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestServiceConfig {
    @Bean
    public OauthTokenService fakeOauthTokenService() {
        return new OauthTokenService() {
            @Override
            public String storeToken(String userId, String accessToken) {
                return null;
            }

            @Override
            public String updateToken(String userId, String accessToken) {
                return null;
            }

            @Override
            public String getRefreshToken(String userId) {
                return null;
            }

        };
    }

    @Bean
    public ConfigService fakeConfigService() {
        return key -> {
            switch (key) {
                case "canvas_url" :
                    return "someDomain";
                default:
                    return "";
            }
        };
    }

    @Bean
    public LtiLaunchKeyService fakeLtiLaunchKeyService() {
        return new LtiLaunchKeyService() {
            @Override
            public String findSecretForKey(String key) {
                return null;
            }
        };
    }

    @Bean
    public LtiLaunch ltiLaunch() {
        return new LtiLaunch();
    }

    @Bean
    public CanvasApiFactory canvasApiFactory() {
        return new CanvasApiFactory("");
    }

}
