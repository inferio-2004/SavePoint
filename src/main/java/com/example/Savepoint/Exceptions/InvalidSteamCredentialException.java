package com.example.Savepoint.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


public class InvalidSteamCredentialException extends RuntimeException {
    public InvalidSteamCredentialException(String message) {
        super(message);
    }
}
