package com.coupon.exception;

public class CouponExhaustedException extends BusinessException {
    public CouponExhaustedException(String message) {
        super(ErrorCode.COUPON_EXHAUSTED, message);
    }
}
