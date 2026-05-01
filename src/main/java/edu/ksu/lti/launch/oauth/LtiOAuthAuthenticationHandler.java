package edu.ksu.lti.launch.oauth;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class LtiOAuthAuthenticationHandler {

    private static final Logger LOG = LogManager.getLogger(LtiOAuthAuthenticationHandler.class);

    public Authentication createAuthentication(HttpServletRequest request, String consumerKey) {
        LOG.debug("Creating LTI authentication for Canvas user " + request.getParameter("custom_canvas_user_login_id"));

        //If we don't pass in the empty set, the resulting object is not considered authenticated (See documentation on this constructor)
        return new UsernamePasswordAuthenticationToken(consumerKey, null, Collections.emptySet());
    }
}
