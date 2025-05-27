package com.coupon.repository;

public interface CouponIssueRepositoryCustom {
    boolean existsByUserIdAndCouponIdToday(Long userId, Long couponId);
}
