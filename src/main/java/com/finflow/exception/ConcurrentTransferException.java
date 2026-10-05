package com.finflow.exception;

/**
 * Thrown when a fund transfer fails after exhausting all retry attempts
 * due to repeated optimistic locking conflicts (concurrent modification
 * of the same Account row).
 */
public class ConcurrentTransferException extends RuntimeException {

    public ConcurrentTransferException(String message) {
        super(message);
    }

    public ConcurrentTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}