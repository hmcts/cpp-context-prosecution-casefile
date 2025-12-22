package uk.gov.moj.cpp.prosecution.casefile.event.listener.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(final String message) {
        super(message);
    }
}
