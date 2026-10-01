package com.xinyue.atelier.exceptions;

public class ResourceNotFoundException extends Exception {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}