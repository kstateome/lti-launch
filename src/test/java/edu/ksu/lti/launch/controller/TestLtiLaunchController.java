package edu.ksu.lti.launch.controller;

import org.springframework.stereotype.Controller;
/* Simply a concrete implementation of LtiLaunchController to test spring
 * the spring context
 */
@Controller
public class TestLtiLaunchController extends LtiLaunchController {
    @Override
    protected String getInitialViewPath() {
        return null;
    }

    @Override
    protected String getApplicationName() {
        return null;
    }
}
