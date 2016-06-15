package edu.ksu.lti.launch.oauth;

import org.apache.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth.provider.OAuthProcessingFilterEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Displays a custom error message when oauth signature verification fails
 */
@Component
public class MyOAuthProcessingFilterEntryPointImpl extends OAuthProcessingFilterEntryPoint {

    private static final Logger LOG = Logger.getLogger(MyOAuthProcessingFilterEntryPointImpl.class);

    /** Called when there is an authentication failure. For LTI applications this means there
     * was a problem verifying the oauth signature. Inform the user to try again or call someone
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        LOG.info("OAuth FILTER Failure (commence), req=" + request + ", ex=" + authException);

        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        StringBuffer sb = new StringBuffer();
        sb.append("<html><head><title>LTI Launch error</title></head>\n");
        sb.append("<body>\n");
        sb.append("<h2>");
        sb.append("LTI Launch error");
        sb.append("</h2>\n");
        sb.append("<p>");
        sb.append("There was an error authenticating this LTI launch request. Please re-launch from within Canvas.");
        sb.append("If the error persists, contact the help desk.");
        sb.append("</p>\n<p>");
        sb.append("Error Details: ");
        sb.append(authException.getMessage());
        sb.append("</p>\n");
        sb.append("</body>");
        sb.append("</html>");

        response.getWriter().println(sb.toString());
    }
}
