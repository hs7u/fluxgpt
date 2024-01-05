package com.openai.service.retry;

/**
 * only this exception can retry
 */
public class RetryThrowable extends Exception {

    public RetryThrowable(String message) {
        super(message);
    }
}
