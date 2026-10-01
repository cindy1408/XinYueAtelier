package com.xinyue.atelier.exceptions;


public class StorageException extends Exception {
    public StorageException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
