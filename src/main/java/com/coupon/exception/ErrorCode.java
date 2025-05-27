package com.coupon.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid Input Value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "Method Not Allowed"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "Entity Not Found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "Internal Server Error"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "Invalid Type Value"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "Access is Denied"),
    
    // User
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "U001", "Email is Duplicated"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "User Not Found"),
    
    // Coupon
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Coupon Not Found"),
    COUPON_ALREADY_ISSUED(HttpStatus.BAD_REQUEST, "P002", "Coupon Already Issued"),
    COUPON_EXHAUSTED(HttpStatus.BAD_REQUEST, "P003", "Coupon is Exhausted"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "P004", "Coupon is Expired"),
    DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "P005", "Daily Limit Exceeded"),
    
    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Unauthorized");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
