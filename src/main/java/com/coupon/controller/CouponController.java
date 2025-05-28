package com.coupon.controller;

import com.coupon.dto.ApiResponse;
import com.coupon.dto.coupon.CouponCreateRequest;
import com.coupon.dto.coupon.CouponIssueResponse;
import com.coupon.dto.coupon.CouponResponse;
import com.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponResponse> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        CouponResponse response = couponService.createCoupon(request);
        return ApiResponse.success(response, "쿠폰이 생성되었습니다.");
    }

    @PostMapping("/{couponId}/issue")
    public ApiResponse<Void> issueCoupon(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long couponId) {
        couponService.issueCoupon(userId, couponId);
        return ApiResponse.success(null, "쿠폰이 발급되었습니다.");
    }

    @GetMapping
    public ApiResponse<List<CouponResponse>> getAllCoupons() {
        List<CouponResponse> coupons = couponService.getAllCoupons();
        return ApiResponse.success(coupons);
    }
    
    /**
     * 사용자가 발급받은 쿠폰 목록을 조회합니다.
     * 
     * @param userId 사용자 ID (헤더에서 추출)
     * @return 발급받은 쿠폰 목록
     */
    @GetMapping("/my")
    public ApiResponse<List<CouponIssueResponse>> getMyIssuedCoupons(
            @RequestHeader("X-USER-ID") Long userId) {
        List<CouponIssueResponse> issuedCoupons = couponService.getIssuedCoupons(userId);
        return ApiResponse.success(issuedCoupons);
    }
}
