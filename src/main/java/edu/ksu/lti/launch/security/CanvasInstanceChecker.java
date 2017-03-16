package edu.ksu.lti.launch.security;

import edu.ksu.lti.launch.exception.InvalidInstanceException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Checks which Canvas instance this application is running in.
 * When the periodic overwrite of beta/test happens in hosted Canvas instances,
 * you can end up with Canvas test pointing at production LTI applications. This
 * can potentially result in test data being pushed into production SIS or other
 * systems that this LTI application may interact with.
 * <p>
 * This checker is invoked during launch and will throw an exception if the
 * Canvas URL coming in from the LTI launch request doesn't match the configured
 * Canvas URL or secondary URL if you have a vanity domain.
 *
 */
@Component
public class CanvasInstanceChecker {
    private final ConfigService configService;

    @Autowired
    public CanvasInstanceChecker(ConfigService configService) {
        this.configService = configService;
    }

    public void assertValidInstance(LtiSession ltiSession) {
        String launchUrl = removeTrailingSlash(ltiSession.getCanvasDomain());
        String canvasUrl = configService.getConfigValue("canvas_url");
        String secondCanvasUrl = configService.getConfigValue("canvas_url_2");
        String canvasDomain = domainFromUrl(canvasUrl);
        String secondCanvasDomain = domainFromUrl(secondCanvasUrl);
        if (!StringUtils.isBlank(secondCanvasDomain)) {
            if (!launchUrl.equalsIgnoreCase(canvasDomain) && !launchUrl.equalsIgnoreCase(secondCanvasDomain)) {
                throw new InvalidInstanceException(launchUrl, ltiSession.getCanvasCourseId());
            }
        } else {
            if (!launchUrl.equalsIgnoreCase(canvasDomain)) {
                throw new InvalidInstanceException(launchUrl, ltiSession.getCanvasCourseId());
            }
        }
    }

    private String domainFromUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        url = removeProtocol(url);
        return removeTrailingSlash(url);
    }

    private String removeProtocol(String url) {
        int firstSlashIndex = url.indexOf("/");
        return url.substring(firstSlashIndex + 2, url.length());
    }

    private String removeTrailingSlash(String url) {
        if (url.charAt(url.length() - 1) == '/') {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
