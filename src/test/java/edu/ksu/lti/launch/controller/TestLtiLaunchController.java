package edu.ksu.lti.launch.controller;

import edu.ksu.lti.launch.security.CanvasInstanceChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
/* Simply a concrete implementation of LtiLaunchController to test
 * the spring context
 */
@Controller
public class TestLtiLaunchController extends LtiLaunchController {

    @Autowired
    public TestLtiLaunchController(CanvasInstanceChecker canvasInstanceChecker) {
        super(canvasInstanceChecker);
    }

    @Override
    protected String getInitialViewPath() {
        return null;
    }

    @Override
    protected String getApplicationName() {
        return null;
    }
}
