package com.coupon.exception;

public class CouponExpiredException extends BusinessException {
    public CouponExpiredException(String message) {
        super(ErrorCode.COUPON_EXPIRED, message);
    }
}
