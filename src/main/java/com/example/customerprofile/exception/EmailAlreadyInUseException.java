package com.example.customerprofile.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("Email " + email + " is already used by another customer");
    }
}
