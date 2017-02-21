package edu.ksu.lti.launch.exception;


public class CookieUnavailableException extends Exception {
    public CookieUnavailableException() {
        // Nothing to do here.
    }

    public CookieUnavailableException(String message) {
        super(message);
    }

    public CookieUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
