package com.example.Savepoint.Exceptions;

import org.springframework.web.bind.annotation.ResponseStatus;


public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
