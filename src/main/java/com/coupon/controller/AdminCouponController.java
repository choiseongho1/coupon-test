package com.coupon.controller;

import com.coupon.dto.ApiResponse;
import com.coupon.dto.coupon.CouponStatisticsResponse;
import com.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;
    
    /**
     * 특정 쿠폰의 발급 통계를 조회합니다.
     * 
     * @param couponId 쿠폰 ID
     * @return 쿠폰 발급 통계 정보
     */
    @GetMapping("/{couponId}/statistics")
    public ApiResponse<CouponStatisticsResponse> getCouponStatistics(@PathVariable Long couponId) {
        CouponStatisticsResponse statistics = couponService.getCouponStatistics(couponId);
        return ApiResponse.success(statistics);
    }
    
    /**
     * 전체 쿠폰 발급 현황 통계를 조회합니다.
     * 
     * @return 전체 쿠폰 발급 통계 정보
     */
    @GetMapping("/statistics")
    public ApiResponse<CouponStatisticsResponse> getAllCouponsStatistics() {
        CouponStatisticsResponse statistics = couponService.getAllCouponsStatistics();
        return ApiResponse.success(statistics);
    }
}
