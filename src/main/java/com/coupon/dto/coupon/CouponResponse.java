package com.coupon.dto.coupon;

import com.coupon.domain.coupon.Coupon;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponResponse {
    private final Long id;
    private final String title;
    private final int totalQuantity;
    private final int remainingQuantity;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime validFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime validTo;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    public CouponResponse(Coupon coupon) {
        this.id = coupon.getId();
        this.title = coupon.getTitle();
        this.totalQuantity = coupon.getTotalQuantity();
        this.remainingQuantity = coupon.getRemainingQuantity();
        this.validFrom = coupon.getValidFrom();
        this.validTo = coupon.getValidTo();
        this.createdAt = coupon.getCreatedAt();
    }
}
