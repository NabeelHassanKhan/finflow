package com.finflow.exception;

public class DailyTransferLimitExceededException extends RuntimeException {

    public DailyTransferLimitExceededException(String message) {
        super(message);
    }
}