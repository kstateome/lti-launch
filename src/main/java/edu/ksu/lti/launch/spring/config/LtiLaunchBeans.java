package edu.ksu.lti.launch.spring.config;

import edu.ksu.lti.launch.util.CanvasResponseParser;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class LtiLaunchBeans {
    @Bean
    public CanvasResponseParser canvasResponseParser() {
        return new CanvasResponseParser();
    }


    @Bean
    @Scope("prototype")
    public HttpClientBuilder httpClientBuilder() {
        return HttpClientBuilder.create();
    }
}
