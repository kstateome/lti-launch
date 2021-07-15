package edu.ksu.lti.launch.controller;

import edu.ksu.lti.launch.model.LtiLaunchData;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.security.CanvasInstanceChecker;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;

/**
 * Class to handle the initial launch of an LTI application and creating a
 * session with authentication information from Canvas in it. After the
 * LTI data's signature is verified it will forward the request to an 
 * "initial view" that all implementing classes must supply via the 
 * getInitialViewPath() method. This initial view method should verify
 * that there is a valid ltiSession with an eID in it and can then serve
 * up its content.
 */
public abstract class LtiLaunchController {
    private static final Logger LOG = Logger.getLogger(LtiLaunchController.class);
    @Autowired
    private CanvasInstanceChecker instanceChecker;

    @RequestMapping(value = "/launch", method = RequestMethod.POST)
    public String ltiLaunch(@ModelAttribute LtiLaunchData ltiData, HttpSession session) throws Exception {
        // Invalidate the session to clear out any old data
        session.invalidate();
        LOG.debug("launch!");
        String canvasCourseId = ltiData.getCustom_canvas_course_id();
        String eID = ltiData.getCustom_canvas_user_login_id();
        LtiSession ltiSession = new LtiSession();
        ltiSession.setApplicationName(getApplicationName());
        ltiSession.setInitialViewPath(getInitialViewPath());
        ltiSession.setEid(eID);
        ltiSession.setCanvasCourseId(canvasCourseId);
        ltiSession.setCanvasDomain(ltiData.getCustom_canvas_api_domain());
        ltiSession.setLtiLaunchData(ltiData);
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpSession newSession = sra.getRequest().getSession();
        String jsessionid = newSession.getId();
        newSession.setAttribute(LtiSession.class.getName(), ltiSession);
        instanceChecker.assertValidInstance(ltiSession);
        LOG.info("launching LTI integration '" + getApplicationName() + "' from " + ltiSession.getCanvasDomain() + " for course: " + canvasCourseId + " as user " + eID);
        LOG.debug("forwarding user to: " + getInitialViewPath());
        return "redirect:" + getInitialViewPath() + ";jsessionid=" + jsessionid;
    }

    /** return the initial path that the user should be sent
     *  to after authenticating the LTI launch request */
    protected abstract String getInitialViewPath();

    /** The identifier of this LTI application. Used to look up config
     * values in the database and such
     */
    protected abstract String getApplicationName();
}
