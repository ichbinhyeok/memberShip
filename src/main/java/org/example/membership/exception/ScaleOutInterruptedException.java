package org.example.membership.exception;

public class ScaleOutInterruptedException extends RuntimeException {
    public ScaleOutInterruptedException(String message) {
        super(message);
    }
}
