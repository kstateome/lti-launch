package edu.ksu.lti.launch.exception;


public class CookieUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

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
