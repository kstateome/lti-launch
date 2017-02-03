package edu.ksu.lti.launch.exception;

/**
 * Thrown if a request comes in without a valid session.
 *
 * If the user doesn't have a session they will need to re-launch the LTI application.
 */
public class NoLtiSessionException extends Exception {
    private static final long serialVersionUID = 1L;
}
