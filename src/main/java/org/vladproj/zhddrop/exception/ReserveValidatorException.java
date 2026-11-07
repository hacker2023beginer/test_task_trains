package org.vladproj.zhddrop.exception;

public class ReserveValidatorException extends RuntimeException {
    public ReserveValidatorException() {
        super();
    }

    public ReserveValidatorException(String message) {
        super(message);
    }

    public ReserveValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReserveValidatorException(Throwable cause) {
        super(cause);
    }
}
