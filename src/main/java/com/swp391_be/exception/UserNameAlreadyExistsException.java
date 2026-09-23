package com.swp391_be.exception;

public class UserNameAlreadyExistsException extends RuntimeException {
    public UserNameAlreadyExistsException(String userName) { super("Username already exists: " + userName); }
}
