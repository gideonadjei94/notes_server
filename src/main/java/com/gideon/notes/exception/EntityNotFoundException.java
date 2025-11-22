package com.gideon.notes.exception;


public class EntityNotFoundException extends RuntimeException{
    public EntityNotFoundException(String msg){
        super(msg);
    }
}
