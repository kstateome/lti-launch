package edu.ksu.lti.launch.spring.config;

import edu.ksu.lti.launch.security.LtiLaunchOAuth1AuthenticationFilter;
import edu.ksu.lti.launch.service.ConfigService;
import edu.ksu.lti.launch.service.LtiLaunchKeyService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.frameoptions.StaticAllowFromStrategy;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

import java.net.URI;

/**
 * This configuration class sets up Spring Security to authenticate LTI
 * launch requests based on the OAuth signature present in the POST params.
 * It also sets up some common HTTP headers that get returned to the browser
 * on each request to make browsers happy running inside of an iframe.
 */
@Configuration
@EnableWebSecurity
public class LtiLaunchSecurityConfig {

    private static final Logger LOG = LogManager.getLogger(LtiLaunchSecurityConfig.class);

    @Configuration
    public static class LTISecurityConfigurerAdapter {
        @Autowired
        private LtiLaunchKeyService ltiLaunchKeyService;

        @Autowired
        private LtiLaunchOAuth1AuthenticationFilter ltiLaunchOAuth1AuthenticationFilter;

        @Autowired
        private ConfigService configService;

        @Bean
        @Order(1)
        public SecurityFilterChain ltiSecurityFilterChain(HttpSecurity http) throws Exception {
            LOG.debug("configuring HttpSecurity");
            String canvasUrl = configService.getConfigValue("canvas_url");
            if (StringUtils.isBlank(canvasUrl)) {
                throw new RuntimeException("Missing canvas_url config value");
            }
            http.securityMatcher("/launch")
                .addFilterBefore(ltiLaunchOAuth1AuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> {
                    headers.frameOptions(frameOptions -> frameOptions.disable());
                    headers.addHeaderWriter(new XFrameOptionsHeaderWriter(new StaticAllowFromStrategy(URI.create(canvasUrl))));
                    headers.addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
                        "default-src 'self' https://s.ksucloud.net https://*.instructure.com; " +
                        "font-src 'self' https://s.ksucloud.net https://*.instructure.com; " +
                        "script-src 'self' 'unsafe-inline' https://ajax.googleapis.com; " +
                        "style-src 'self' 'unsafe-inline' https://*.instructure.com https://www.k-state.edu"));
                    headers.addHeaderWriter(new StaticHeadersWriter("P3P",
                        "CP=\"This is just to make IE happy with cookies in this iframe\""));
                });

            return http.build();
        }

        @Bean
        public LtiLaunchOAuth1AuthenticationFilter ltiLaunchOAuth1AuthenticationFilter() {
            return new LtiLaunchOAuth1AuthenticationFilter(ltiLaunchKeyService);
        }
    }
}
