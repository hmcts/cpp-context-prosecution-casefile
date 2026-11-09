package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

/**
 * Thrown when case management does not accept a create-group-case call. Letting it propagate out of
 * the event processor makes the framework roll back and redeliver the event, and then dead-letter it.
 */
public class CaseManagementException extends RuntimeException {

    public static final int NO_STATUS = -1;

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public CaseManagementException(final String message, final int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CaseManagementException(final String message, final Throwable cause) {
        super(message, cause);
        this.statusCode = NO_STATUS;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
