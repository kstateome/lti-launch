package edu.ksu.lti.launch.security;

import edu.ksu.lti.launch.service.LtiLaunchKeyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

public class LtiLaunchOAuth1AuthenticationFilterUTest {

    private static final String CONSUMER_KEY = "key-1";
    private static final String CONSUMER_SECRET = "secret-1";

    @After
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void acceptsValidLaunchRequestAndAuthenticates() throws Exception {
        LtiLaunchOAuth1AuthenticationFilter filter = new LtiLaunchOAuth1AuthenticationFilter(keyService());
        MockHttpServletRequest request = newValidLaunchRequest("nonce-valid");
        request.addParameter("oauth_signature", signatureFor(request, CONSUMER_SECRET));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        Assert.assertEquals(CONSUMER_KEY, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    public void rejectsInvalidSignature() throws Exception {
        LtiLaunchOAuth1AuthenticationFilter filter = new LtiLaunchOAuth1AuthenticationFilter(keyService());
        MockHttpServletRequest request = newValidLaunchRequest("nonce-bad-signature");
        request.addParameter("oauth_signature", "not-a-real-signature");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Assert.assertEquals(401, response.getStatus());
        Assert.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void rejectsNonceReplay() throws Exception {
        LtiLaunchOAuth1AuthenticationFilter filter = new LtiLaunchOAuth1AuthenticationFilter(keyService());
        String nonce = "nonce-replay";

        MockHttpServletRequest firstRequest = newValidLaunchRequest(nonce);
        firstRequest.addParameter("oauth_signature", signatureFor(firstRequest, CONSUMER_SECRET));

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());
        Assert.assertEquals(200, firstResponse.getStatus());

        MockHttpServletRequest replayRequest = newValidLaunchRequest(nonce);
        replayRequest.setParameter("oauth_timestamp", firstRequest.getParameter("oauth_timestamp"));
        replayRequest.addParameter("oauth_signature", signatureFor(replayRequest, CONSUMER_SECRET));

        MockHttpServletResponse replayResponse = new MockHttpServletResponse();
        filter.doFilter(replayRequest, replayResponse, new MockFilterChain());

        Assert.assertEquals(401, replayResponse.getStatus());
    }

    private static LtiLaunchKeyService keyService() {
        return key -> CONSUMER_KEY.equals(key) ? CONSUMER_SECRET : null;
    }

    private static MockHttpServletRequest newValidLaunchRequest(String nonce) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setScheme("https");
        request.setServerName("canvas.example.edu");
        request.setServerPort(443);
        request.setRequestURI("/launch");
        request.setServletPath("/launch");

        request.addParameter("oauth_consumer_key", CONSUMER_KEY);
        request.addParameter("oauth_signature_method", "HMAC-SHA1");
        request.addParameter("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        request.addParameter("oauth_nonce", nonce);
        request.addParameter("oauth_version", "1.0");

        // LTI launch payload fields should be part of the signed parameter set.
        request.addParameter("custom_canvas_user_login_id", "user1");
        request.addParameter("custom_canvas_course_id", "course1");
        return request;
    }

    private static String signatureFor(MockHttpServletRequest request, String consumerSecret) throws Exception {
        String method = request.getMethod().toUpperCase();
        String normalizedUrl = request.getScheme().toLowerCase() + "://" + request.getServerName().toLowerCase() + request.getRequestURI();

        List<String> pairs = new ArrayList<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if ("oauth_signature".equals(name)) {
                continue;
            }
            String[] values = request.getParameterValues(name);
            if (values == null || values.length == 0) {
                pairs.add(percentEncode(name) + "=");
                continue;
            }
            for (String value : values) {
                pairs.add(percentEncode(name) + "=" + percentEncode(value));
            }
        }
        pairs.sort(Comparator.naturalOrder());

        String normalizedParameters = String.join("&", pairs);
        String baseString = percentEncode(method) + '&' + percentEncode(normalizedUrl) + '&' + percentEncode(normalizedParameters);
        String signingKey = percentEncode(consumerSecret) + '&';

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
    }
}

