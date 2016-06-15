package edu.ksu.lti.launch.exception;

public class InvalidInstanceException extends RuntimeException {
    public final String launchUrl;
    public final String courseId;

    public InvalidInstanceException(String launchUrl, String courseId) {
        this.launchUrl = launchUrl;
        this.courseId = courseId;
    }
}
