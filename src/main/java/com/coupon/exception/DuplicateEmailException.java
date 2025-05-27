package com.coupon.exception;

public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException(String message) {
        super(ErrorCode.DUPLICATE_EMAIL, message);
    }
}
