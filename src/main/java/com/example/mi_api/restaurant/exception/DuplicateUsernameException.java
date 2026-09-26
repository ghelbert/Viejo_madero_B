package com.example.mi_api.restaurant.exception;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException() {
        super("Ese nombre de usuario ya está registrado.");
    }
}