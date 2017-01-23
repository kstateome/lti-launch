package edu.ksu.lti.launch.exception;

/**
 * An exception to be thrown when we fail to refresh an OAuth token for an unexpected reason
 * @author alexanda
 * @author ssnelson
 */
public class RefreshFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RefreshFailedException(String message, Throwable cause){
        super(message, cause);
    }

}
