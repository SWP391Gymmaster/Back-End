package com.swp391_be.exception;

// Trả về HTTP 400 khi dữ liệu vi phạm quy tắc nghiệp vụ (trùng lịch, sai giờ...)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
