package com.example.Savepoint.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


public class SteamUserNotFoundException extends RuntimeException {
    public SteamUserNotFoundException(String message) {
        super(message);
    }
}
