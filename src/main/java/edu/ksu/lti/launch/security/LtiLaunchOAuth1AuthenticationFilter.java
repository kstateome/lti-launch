package edu.ksu.lti.launch.security;

import edu.ksu.lti.launch.service.LtiLaunchKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight OAuth 1.0a verifier for LTI launch requests on /launch.
 */
public class LtiLaunchOAuth1AuthenticationFilter extends OncePerRequestFilter {

    private static final long NONCE_VALIDITY_SECONDS = 600;

    private final LtiLaunchKeyService ltiLaunchKeyService;
    private final Map<String, Long> seenNonces = new ConcurrentHashMap<>();

    public LtiLaunchOAuth1AuthenticationFilter(LtiLaunchKeyService ltiLaunchKeyService) {
        this.ltiLaunchKeyService = ltiLaunchKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && "/launch".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        String consumerKey = request.getParameter("oauth_consumer_key");
        String signatureMethod = request.getParameter("oauth_signature_method");
        String signature = request.getParameter("oauth_signature");
        String timestamp = request.getParameter("oauth_timestamp");
        String nonce = request.getParameter("oauth_nonce");

        if (StringUtils.isAnyBlank(consumerKey, signatureMethod, signature, timestamp, nonce)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing OAuth launch parameters");
            return;
        }

        if (!"HMAC-SHA1".equalsIgnoreCase(signatureMethod)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported OAuth signature method");
            return;
        }

        String secret = ltiLaunchKeyService.findSecretForKey(consumerKey);
        if (StringUtils.isBlank(secret)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown OAuth consumer key");
            return;
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid OAuth timestamp");
            return;
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        if (Math.abs(nowSeconds - timestampSeconds) > NONCE_VALIDITY_SECONDS) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth timestamp is outside validity window");
            return;
        }

        String nonceKey = consumerKey + ':' + nonce + ':' + timestamp;
        evictExpiredNonces(nowSeconds);
        if (seenNonces.putIfAbsent(nonceKey, nowSeconds) != null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth nonce has already been used");
            return;
        }

        String expectedSignature;
        try {
            expectedSignature = calculateSignature(request, secret);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Could not verify OAuth signature");
            return;
        }

        if (!constantTimeEquals(signature, expectedSignature)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid OAuth signature");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(consumerKey, null, Collections.emptySet())
        );

        filterChain.doFilter(request, response);
    }

    private String calculateSignature(HttpServletRequest request, String consumerSecret) throws Exception {
        String method = request.getMethod().toUpperCase();
        String normalizedUrl = request.getScheme().toLowerCase() + "://" + request.getServerName().toLowerCase()
            + getPortPart(request) + request.getRequestURI();

        List<String> pairs = new ArrayList<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
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

    private void evictExpiredNonces(long nowSeconds) {
        long expiry = nowSeconds - NONCE_VALIDITY_SECONDS;
        for (Map.Entry<String, Long> entry : seenNonces.entrySet()) {
            if (entry.getValue() < expiry) {
                seenNonces.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
    }

    private static String getPortPart(HttpServletRequest request) {
        int port = request.getServerPort();
        if (("http".equalsIgnoreCase(request.getScheme()) && port == 80)
            || ("https".equalsIgnoreCase(request.getScheme()) && port == 443)) {
            return "";
        }
        return ":" + port;
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(leftBytes, rightBytes);
    }
}

