package org.vladproj.zhddrop.exception;

public class TripServiceException extends RuntimeException {
    public TripServiceException(String message) {
        super(message);
    }

    public TripServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    protected TripServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TripServiceException(Throwable cause) {
        super(cause);
    }
}
