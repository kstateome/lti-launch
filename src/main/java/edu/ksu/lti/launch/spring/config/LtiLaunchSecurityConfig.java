package edu.ksu.lti.launch.spring.config;

import edu.ksu.lti.launch.oauth.LtiConsumerDetailsService;
import edu.ksu.lti.launch.oauth.LtiOAuthAuthenticationHandler;
import edu.ksu.lti.launch.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.oauth.provider.filter.ProtectedResourceProcessingFilter;
import org.springframework.security.oauth.provider.nonce.InMemoryNonceServices;
import org.springframework.security.oauth.provider.token.InMemoryProviderTokenServices;
import org.springframework.security.oauth.provider.token.OAuthProviderTokenServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.frameoptions.StaticAllowFromStrategy;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.net.URI;

/**
 * This configuration class sets up Spring Security to authenticate LTI
 * launch requests based on the OAuth signature present in the POST params.
 * It also sets up some common HTTP headers that get returned to the browser
 * on each request to make browsers happy running inside of an iframe.
 */
@Configuration
@EnableWebMvcSecurity
public class LtiLaunchSecurityConfig extends WebMvcConfigurerAdapter {

    private static final Logger LOG = LogManager.getLogger(LtiLaunchSecurityConfig.class);

    @Configuration
    @Order(1)
    public static class LTISecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        @Autowired
        private LtiConsumerDetailsService oauthConsumerDetailsService;
        @Autowired
        private LtiOAuthAuthenticationHandler oauthAuthenticationHandler;
        @Autowired
        private OAuthProviderTokenServices oauthProviderTokenServices;

        @Autowired
        private ConfigService configService;

        @Override
        public void configure(WebSecurity web) throws Exception {
            //security debugging should not be used in production!
            //You probably won't even want it in development most of the time but I'll leave it here for reference.
            /*
            if(LOG.isDebugEnabled()) {
                web.debug(true);
            }
            */
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            LOG.debug("configuring HttpSecurity");
            String canvasUrl = configService.getConfigValue("canvas_url");
            if (StringUtils.isBlank(canvasUrl)) {
                throw new RuntimeException("Missing canvas_url config value");
            }
            http.securityMatchers()
                .requestMatchers("/launch").and()
                .addFilterBefore(configureProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests().anyRequest().authenticated().and().csrf().disable()
                .headers()
                .frameOptions()
                .disable()
                .addHeaderWriter(new XFrameOptionsHeaderWriter(new StaticAllowFromStrategy(new URI(canvasUrl))))
                .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
                        "default-src 'self' https://s.ksucloud.net https://*.instructure.com; " +
                        "font-src 'self' https://s.ksucloud.net https://*.instructure.com; " +
                        "script-src 'self' 'unsafe-inline' https://ajax.googleapis.com; " +
                        "style-src 'self' 'unsafe-inline' https://*.instructure.com https://www.k-state.edu" ))
                .addHeaderWriter(new StaticHeadersWriter("P3P", "CP=\"This is just to make IE happy with cookies in this iframe\""));
        }

        private ProtectedResourceProcessingFilter configureProcessingFilter() {
            //Set up nonce service to prevent replay attacks.
            InMemoryNonceServices nonceService = new InMemoryNonceServices();
            nonceService.setValidityWindowSeconds(600);

            ProtectedResourceProcessingFilter processingFilter = new ProtectedResourceProcessingFilter();
            processingFilter.setAuthHandler(oauthAuthenticationHandler);
            processingFilter.setConsumerDetailsService(oauthConsumerDetailsService);
            processingFilter.setNonceServices(nonceService);
            processingFilter.setTokenServices(oauthProviderTokenServices);
            return processingFilter;
        }
    }

    @Bean(name = "oauthProviderTokenServices")
    public OAuthProviderTokenServices oauthProviderTokenServices() {
        // NOTE: we don't use the OAuthProviderTokenServices for 0-legged but it cannot be null
        return new InMemoryProviderTokenServices();
    }
}
