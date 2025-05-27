package com.coupon.exception;

public class DailyLimitExceededException extends BusinessException {
    public DailyLimitExceededException(String message) {
        super(ErrorCode.DAILY_LIMIT_EXCEEDED, message);
    }
}
