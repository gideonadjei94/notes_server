package com.gideon.notes.exception;

public class ExpiredAuthTokenException extends RuntimeException{
    public ExpiredAuthTokenException(String msg){
        super(msg);
    }
}
