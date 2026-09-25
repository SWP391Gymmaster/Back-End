package com.swp391_be.exception;

// Trả về HTTP 404 khi không tìm thấy dữ liệu
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
