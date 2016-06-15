package edu.ksu.lti.launch.spring.config;

import edu.ksu.lti.launch.oauth.LTIConsumerDetailsService;
import edu.ksu.lti.launch.oauth.LTIOAuthAuthenticationHandler;
import edu.ksu.lti.launch.oauth.LTIOAuthProviderProcessingFilter;
import edu.ksu.lti.launch.oauth.MyOAuthNonceServices;
import edu.ksu.lti.launch.service.ConfigService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.oauth.provider.OAuthProcessingFilterEntryPoint;
import org.springframework.security.oauth.provider.token.InMemoryProviderTokenServices;
import org.springframework.security.oauth.provider.token.OAuthProviderTokenServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.frameoptions.StaticAllowFromStrategy;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import java.net.URI;

/**
 * Common LTI application configuration. A few things that are done here:
 *  - Enable web security on the /launch URL to verify the oauth signature coming from Canvas
 *  - Enable content security policy headers so that browsers are happy
 *  - Set up the JSP based view resolver for spring
 *  - Component scanning for spring beans and JPA repositories
 * @author toby
 *
 */
@Configuration
@EnableWebMvcSecurity
@PropertySource("classpath:application.properties")
public class LtiLaunchSecurityConfig extends WebMvcConfigurerAdapter implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = Logger.getLogger(LtiLaunchSecurityConfig.class);

    @Autowired
    Environment env;

    private static volatile ApplicationContext context;

    @Configuration
    @Order(1)
    public static class LTISecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        private LTIOAuthProviderProcessingFilter ltioAuthProviderProcessingFilter;
        @Autowired
        private LTIConsumerDetailsService oauthConsumerDetailsService;
        @Autowired
        private MyOAuthNonceServices oauthNonceServices;
        @Autowired
        private LTIOAuthAuthenticationHandler oauthAuthenticationHandler;
        @Autowired
        private OAuthProcessingFilterEntryPoint oauthProcessingFilterEntryPoint;
        @Autowired
        private OAuthProviderTokenServices oauthProviderTokenServices;

        @Autowired
        private ConfigService configRepo;

        @PostConstruct
        public void init() {
            ltioAuthProviderProcessingFilter = new LTIOAuthProviderProcessingFilter(oauthConsumerDetailsService, oauthNonceServices, oauthProcessingFilterEntryPoint, oauthAuthenticationHandler, oauthProviderTokenServices);
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            //security debugging should not be used in production!
            //You probably won't even want it in development most of the time but I'll leave it here for reference.
            if(LOG.isDebugEnabled()) {
                //web.debug(true);
            }
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            LOG.debug("configuring HttpSecurity");
            String canvasUrl = configRepo.getConfigValue("canvas_url");
            if(canvasUrl == null) {
                throw new RuntimeException("Missing canvas_url config value");
            }
            http.requestMatchers()
                .antMatchers("/launch").and()
                .addFilterBefore(ltioAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests().anyRequest().hasRole("LTI").and().csrf().disable()
                .headers().addHeaderWriter(new XFrameOptionsHeaderWriter(new StaticAllowFromStrategy(new URI(canvasUrl))))
                .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
                        "default-src 'self' https://s.ksucloud.net https://*.instructure.com; " +
                        "font-src 'self' https://s.ksucloud.net https://*.instructure.com; " +
                        "script-src 'self' 'unsafe-inline' https://ajax.googleapis.com; " +
                        "style-src 'self' 'unsafe-inline' https://*.instructure.com https://www.k-state.edu" ))
                .addHeaderWriter(new StaticHeadersWriter("P3P", "CP=\"This is just to make IE happy with cookies in this iframe\""));
        }
    }
    
    @Bean(name = "oauthProviderTokenServices")
    public OAuthProviderTokenServices oauthProviderTokenServices() {
        // NOTE: we don't use the OAuthProviderTokenServices for 0-legged but it cannot be null
        return new InMemoryProviderTokenServices();
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOG.info("Context event caught");
        //I was using this to insert records into the DB during testing. Probably not needed much any more
    }
}