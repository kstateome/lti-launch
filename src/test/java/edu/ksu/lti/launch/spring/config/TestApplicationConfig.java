package edu.ksu.lti.launch.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class TestApplicationConfig {
    private static final String APPLICATION_NAME = "testLtiLaunch";

    @Bean
    public String applicationName() {
        return APPLICATION_NAME;
    }

    @Bean
    public DefaultCookieSerializer fakeDefaultCookieSerializer() {
        return new DefaultCookieSerializer();
    }
}
