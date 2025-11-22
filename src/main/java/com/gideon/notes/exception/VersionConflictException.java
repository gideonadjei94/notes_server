package com.gideon.notes.exception;

public class VersionConflictException extends RuntimeException{
    public VersionConflictException(String msg){
        super(msg);
    }
}
