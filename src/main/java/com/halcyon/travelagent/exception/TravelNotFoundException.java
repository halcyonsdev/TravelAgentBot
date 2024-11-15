package com.halcyon.travelagent.exception;

public class TravelNotFoundException extends RuntimeException {
    public TravelNotFoundException(String message) {
        super(message);
    }
}
