package edu.ksu.lti.launch.spring.config;

import edu.ksu.lti.launch.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * OAuth2 client setup for Canvas API authorization-code flow.
 */
@Configuration
public class CanvasOAuth2ClientConfig {

    private static final String CANVAS_REGISTRATION_ID = "canvas";

    @Autowired
    private ConfigService configService;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        String canvasUrl = configService.getConfigValue("canvas_url");
        String clientId = configService.getConfigValue("oauth_client_id");
        String clientSecret = configService.getConfigValue("oauth_client_secret");

        if (StringUtils.isAnyBlank(canvasUrl, clientId, clientSecret)) {
            throw new RuntimeException("Missing canvas OAuth2 configuration values");
        }

        ClientRegistration canvas = ClientRegistration.withRegistrationId(CANVAS_REGISTRATION_ID)
            .clientName("Canvas")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/oauthResponse")
            .authorizationUri(canvasUrl + "/login/oauth2/auth")
            .tokenUri(canvasUrl + "/login/oauth2/token")
            .build();

        return new InMemoryClientRegistrationRepository(canvas);
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> oauth2AccessTokenResponseClient() {
        return new RestClientAuthorizationCodeTokenResponseClient();
    }
}

