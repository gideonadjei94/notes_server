package com.gideon.notes.exception;

public class InvalidAuthTokenException  extends RuntimeException{
    public InvalidAuthTokenException(String msg){
        super(msg);
    }
}
