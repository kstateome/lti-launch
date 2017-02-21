package edu.ksu.lti.launch.exception;


public class CookieUnavailableException extends Exception {
    public CookieUnavailableException() {
    }

    public CookieUnavailableException(String message) {
        super(message);
    }

    public CookieUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
