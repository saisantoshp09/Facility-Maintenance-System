package com.facility.maintenance.application;

/** Raised when a dependency needed for an authorization decision cannot be reached (fail closed). */
public class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
