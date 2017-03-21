package edu.ksu.lti.launch.oauth;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth.provider.ConsumerAuthentication;
import org.springframework.security.oauth.provider.OAuthAuthenticationHandler;
import org.springframework.security.oauth.provider.token.OAuthAccessProviderToken;
import org.springframework.stereotype.Component;

@Component
public class LtiOAuthAuthenticationHandler implements OAuthAuthenticationHandler{

    private static final Logger LOG = Logger.getLogger(LtiOAuthAuthenticationHandler.class);

    @Override
    public Authentication createAuthentication(HttpServletRequest request,
                                               ConsumerAuthentication consumerAuthentication,
                                               OAuthAccessProviderToken authToken) {
        LOG.debug("Creating LTI authentication for Canvas user " + request.getParameter("custom_canvas_user_login_id"));

        //If we don't pass in the empty set, the resulting object is not considered authenticated (See documentation on this constructor)
        return new UsernamePasswordAuthenticationToken(consumerAuthentication.getConsumerCredentials(), null, Collections.emptySet());
    }
}
