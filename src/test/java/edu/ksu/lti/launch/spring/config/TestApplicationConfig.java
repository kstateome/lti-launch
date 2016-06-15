package edu.ksu.lti.launch.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestApplicationConfig {
    public static final String APPLICATION_NAME = "testLtiLaunch";

    @Bean
    public String applicationName() {
        return APPLICATION_NAME;
    }
}
