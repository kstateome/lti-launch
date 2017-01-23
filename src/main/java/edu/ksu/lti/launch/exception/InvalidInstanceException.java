package edu.ksu.lti.launch.exception;

/**
 * Thrown to indicate that this LTI application is running in an unexpected instance of Canvas.
 *
 * This happens most often after a Canvas test/beta overwrite from production. The
 * production settings are copied to the development instance and then you have a user
 * in Canvas test trying to hit the production LTI application.
 */
public class InvalidInstanceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public final String launchUrl;
    public final String courseId;

    public InvalidInstanceException(String launchUrl, String courseId) {
        this.launchUrl = launchUrl;
        this.courseId = courseId;
    }
}
