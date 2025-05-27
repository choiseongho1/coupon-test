package com.coupon.exception;

public class CouponAlreadyIssuedException extends BusinessException {
    public CouponAlreadyIssuedException(String message) {
        super(ErrorCode.COUPON_ALREADY_ISSUED, message);
    }
}
