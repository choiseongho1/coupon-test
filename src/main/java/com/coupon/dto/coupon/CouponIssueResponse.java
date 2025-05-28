package com.coupon.dto.coupon;

import com.coupon.domain.coupon.CouponIssue;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponIssueResponse {
    private final Long id;
    private final Long userId;
    private final Long couponId;
    private final String couponTitle;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime issuedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime validFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime validTo;

    public CouponIssueResponse(CouponIssue couponIssue) {
        this.id = couponIssue.getId();
        this.userId = couponIssue.getUser().getId();
        this.couponId = couponIssue.getCoupon().getId();
        this.couponTitle = couponIssue.getCoupon().getTitle();
        this.issuedAt = couponIssue.getIssuedAt();
        this.validFrom = couponIssue.getCoupon().getValidFrom();
        this.validTo = couponIssue.getCoupon().getValidTo();
    }
    
    public CouponIssueResponse(Long id, Long userId, Long couponId, LocalDateTime issuedAt) {
        this.id = id;
        this.userId = userId;
        this.couponId = couponId;
        this.couponTitle = ""; // This will be set to empty string as we don't have the title in this context
        this.issuedAt = issuedAt;
        this.validFrom = null;
        this.validTo = null;
    }
    
    public CouponIssueResponse(Long id, Long userId, Long couponId, LocalDateTime issuedAt, 
                             String couponTitle, LocalDateTime validFrom, LocalDateTime validTo) {
        this.id = id;
        this.userId = userId;
        this.couponId = couponId;
        this.couponTitle = couponTitle;
        this.issuedAt = issuedAt;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}
