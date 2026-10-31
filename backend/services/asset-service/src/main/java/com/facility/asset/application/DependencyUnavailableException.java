package com.facility.asset.application;

public class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(String message, Throwable cause) { super(message, cause); }
}
