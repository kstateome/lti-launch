package edu.ksu.lti.launch.spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"edu.ksu.lti.launch.oauth", "edu.ksu.lti.launch.security", "edu.ksu.lti.launch.controller", "edu.ksu.lti.launch.spring.config", "edu.ksu.lti.launch.security"})
public class TestSpringConfig {
}
