package com.example.Savepoint.Exceptions;

public class SelfFollowException extends RuntimeException {
    public SelfFollowException() {
        super("Cannot follow yourself");
    }
}
