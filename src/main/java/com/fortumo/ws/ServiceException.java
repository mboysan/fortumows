package com.fortumo.ws;

/**
 * Represents a {@link RuntimeException} thrown by the {@link SumService}.
 */
public class ServiceException extends RuntimeException {
    public ServiceException(Throwable cause) {
        super(cause);
    }
}
