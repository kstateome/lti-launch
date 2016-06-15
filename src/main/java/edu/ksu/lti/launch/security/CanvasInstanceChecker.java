package edu.ksu.lti.launch.security;

import edu.ksu.lti.launch.exception.InvalidInstanceException;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class CanvasInstanceChecker {
    @Autowired
    private ConfigService configService;

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
