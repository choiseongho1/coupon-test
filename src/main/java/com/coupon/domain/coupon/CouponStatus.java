package com.coupon.domain.coupon;

/**
 * 쿠폰의 상태를 나타내는 열거형입니다.
 */
public enum CouponStatus {
    /**
     * 활성화 상태 (발급 가능)
     */
    ACTIVE,
    
    /**
     * 만료된 상태 (발급 불가)
     */
    EXPIRED,
    
    /**
     * 소진된 상태 (재고 없음)
     */
    EXHAUSTED
}
