package edu.ksu.lti.launch.controller;

import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.ConfigService;
import edu.ksu.lti.launch.service.LtiSessionService;
import edu.ksu.lti.launch.service.OauthTokenService;
import edu.ksu.canvas.oauth.OauthToken;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OauthControllerUTest {

    @Test
    public void oauthResponseExchangesCodeAndStoresRefreshToken() throws Exception {
        ConfigService configService = mock(ConfigService.class);
        OauthTokenService oauthTokenService = mock(OauthTokenService.class);
        LtiSessionService ltiSessionService = mock(LtiSessionService.class);
        ClientRegistrationRepository clientRegistrationRepository = mock(ClientRegistrationRepository.class);
        @SuppressWarnings("unchecked")
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient =
            mock(OAuth2AccessTokenResponseClient.class);

        OauthController controller = new OauthController(
            configService,
            oauthTokenService,
            ltiSessionService,
            clientRegistrationRepository,
            tokenClient
        ) {
            @Override
            protected OauthToken createRefreshableOauthToken(String clientId, String clientSecret, String canvasUrl,
                                                             String refreshToken) {
                return mock(OauthToken.class);
            }
        };

        LtiSession ltiSession = new LtiSession();
        ltiSession.setEid("eid1");
        ltiSession.setInitialViewPath("/home");
        ltiSession.setOauthTokenRequestState("state-123");
        when(ltiSessionService.getLtiSession()).thenReturn(ltiSession);

        ClientRegistration registration = ClientRegistration.withRegistrationId("canvas")
            .clientId("client-id")
            .clientSecret("client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri("https://canvas.example.edu/login/oauth2/auth")
            .tokenUri("https://canvas.example.edu/login/oauth2/token")
            .redirectUri("{baseUrl}/oauthResponse")
            .build();
        when(clientRegistrationRepository.findByRegistrationId("canvas")).thenReturn(registration);

        OAuth2AccessTokenResponse tokenResponse = OAuth2AccessTokenResponse.withToken("access-abc")
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .expiresIn(3600)
            .refreshToken("refresh-xyz")
            .build();
        when(tokenClient.getTokenResponse(any(OAuth2AuthorizationCodeGrantRequest.class))).thenReturn(tokenResponse);

        when(configService.getConfigValue("canvas_url")).thenReturn("https://canvas.example.edu");
        when(oauthTokenService.getRefreshToken("eid1")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("app.example.edu");
        request.setServerPort(443);
        request.setContextPath("/lti");

        String result = controller.oauthResponse(request, "auth-code", "state-123", null);

        Assert.assertEquals("redirect:/home", result);
        verify(oauthTokenService).storeToken("eid1", "refresh-xyz");
        Assert.assertNotNull(ltiSession.getOauthToken());

        ArgumentCaptor<OAuth2AuthorizationCodeGrantRequest> grantCaptor =
            ArgumentCaptor.forClass(OAuth2AuthorizationCodeGrantRequest.class);
        verify(tokenClient).getTokenResponse(grantCaptor.capture());
        Assert.assertEquals("auth-code",
            grantCaptor.getValue().getAuthorizationExchange().getAuthorizationResponse().getCode());
    }
}

