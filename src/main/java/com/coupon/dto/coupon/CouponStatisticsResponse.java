package com.coupon.dto.coupon;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 통계 정보를 담는 응답 클래스입니다.
 */
@Getter
@Builder
public class CouponStatisticsResponse {
    
    private final Long couponId;
    private final String couponTitle;
    private final CouponStatus status;
    
    private final Integer totalQuantity;
    private final Integer remainingQuantity;
    private final Integer issuedQuantity;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime validFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime validTo;
    
    private final Long issuedToday;
    private final Long issuedThisWeek;
    private final Long issuedThisMonth;
    
    /**
     * 단일 쿠폰에 대한 통계 정보를 생성합니다.
     * 
     * @param coupon 쿠폰 엔티티
     * @param issuedToday 오늘 발급된 수량
     * @param issuedThisWeek 이번 주 발급된 수량
     * @param issuedThisMonth 이번 달 발급된 수량
     * @return 쿠폰 통계 정보
     */
    public static CouponStatisticsResponse from(
            Coupon coupon, 
            Long issuedToday, 
            Long issuedThisWeek, 
            Long issuedThisMonth) {
        
        return CouponStatisticsResponse.builder()
                .couponId(coupon.getId())
                .couponTitle(coupon.getTitle())
                .status(coupon.getStatus())
                .totalQuantity(coupon.getTotalQuantity())
                .remainingQuantity(coupon.getRemainingQuantity())
                .issuedQuantity(coupon.getTotalQuantity() - coupon.getRemainingQuantity())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .issuedToday(issuedToday)
                .issuedThisWeek(issuedThisWeek)
                .issuedThisMonth(issuedThisMonth)
                .build();
    }
    
    /**
     * 전체 쿠폰에 대한 통계 정보를 생성합니다.
     * 
     * @param totalCoupons 전체 쿠폰 수
     * @param activeCoupons 활성화된 쿠폰 수
     * @param expiredCoupons 만료된 쿠폰 수
     * @param exhaustedCoupons 소진된 쿠폰 수
     * @param totalQuantity 전체 발급 가능 수량
     * @param remainingQuantity 남은 발급 가능 수량
     * @param issuedToday 오늘 발급된 수량
     * @param issuedThisWeek 이번 주 발급된 수량
     * @param issuedThisMonth 이번 달 발급된 수량
     * @return 전체 쿠폰 통계 정보
     */
    public static CouponStatisticsResponse forAllCoupons(
            int totalCoupons,
            int activeCoupons,
            int expiredCoupons,
            int exhaustedCoupons,
            int totalQuantity,
            int remainingQuantity,
            long issuedToday,
            long issuedThisWeek,
            long issuedThisMonth) {
        
        return CouponStatisticsResponse.builder()
                .couponId(null)
                .couponTitle("전체 쿠폰 통계")
                .status(null)
                .totalQuantity(totalQuantity)
                .remainingQuantity(remainingQuantity)
                .issuedQuantity(totalQuantity - remainingQuantity)
                .validFrom(null)
                .validTo(null)
                .issuedToday(issuedToday)
                .issuedThisWeek(issuedThisWeek)
                .issuedThisMonth(issuedThisMonth)
                .build();
    }
}
