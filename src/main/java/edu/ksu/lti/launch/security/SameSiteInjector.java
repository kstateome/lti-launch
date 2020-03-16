package edu.ksu.lti.launch.security;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.stereotype.Component;

@Component
public class SameSiteInjector {
    private static final Logger LOG = Logger.getLogger(SameSiteInjector.class);

    @Autowired
    private ApplicationContext applicationContext;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        DefaultCookieSerializer cookieSerializer = applicationContext.getBean(DefaultCookieSerializer.class);
        LOG.info("Received DefaultCookieSerializer, Overriding SameSite Strict");
        cookieSerializer.setSameSite("None; Secure");
    }
}