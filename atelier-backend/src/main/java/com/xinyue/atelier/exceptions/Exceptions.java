package com.xinyue.atelier.exceptions;

public abstract class Exceptions extends RuntimeException {
    protected Exceptions(String message) { super(message); }
    protected Exceptions(String message, Throwable cause) { super(message, cause); }
}