package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponIssue;
import com.coupon.domain.user.User;
import com.coupon.dto.coupon.CouponCreateRequest;
import com.coupon.dto.coupon.CouponIssueResponse;
import com.coupon.dto.coupon.CouponResponse;
import com.coupon.exception.CouponAlreadyIssuedException;
import com.coupon.exception.CouponExhaustedException;
import com.coupon.exception.CouponExpiredException;
import com.coupon.exception.DailyLimitExceededException;
import com.coupon.repository.CouponIssueRepository;
import com.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final UserService userService;

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        Coupon coupon = Coupon.builder()
                .title(request.getTitle())
                .totalQuantity(request.getTotalQuantity())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        return new CouponResponse(savedCoupon);
    }

    @Transactional
    public CouponIssueResponse issueCoupon(Long userId, Long couponId) {
        // 1. 사용자 조회
        User user = userService.findById(userId);
        
        // 2. 쿠폰 조회 (비관적 락 적용)
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + couponId));
        
        // 3. 쿠폰 유효성 검사
        validateCouponIssue(user, coupon);
        
        // 4. 쿠폰 발급 처리
        CouponIssue couponIssue = CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .issuedAt(LocalDateTime.now())
                .build();
        CouponIssue savedCouponIssue = couponIssueRepository.save(couponIssue);
        
        return new CouponIssueResponse(
                savedCouponIssue.getId(),
                savedCouponIssue.getUser().getId(),
                savedCouponIssue.getCoupon().getId(),
                savedCouponIssue.getIssuedAt()
        );
    }

    private void validateCouponIssue(User user, Coupon coupon) {
        // 1. 쿠폰 기간 검증
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom())) {
            throw new CouponExpiredException("쿠폰 발급 기간이 아닙니다.");
        }
        if (now.isAfter(coupon.getValidTo())) {
            throw new CouponExpiredException("만료된 쿠폰입니다.");
        }
        
        // 2. 재고 확인
        if (coupon.getRemainingQuantity() <= 0) {
            throw new CouponExhaustedException("쿠폰이 모두 소진되었습니다.");
        }
        
        // 3. 중복 발급 방지
        if (couponIssueRepository.existsByUserIdAndCouponId(user.getId(), coupon.getId())) {
            throw new CouponAlreadyIssuedException("이미 발급받은 쿠폰입니다.");
        }
        
        // 4. 1일 1회 발급 제한
        if (couponIssueRepository.existsByUserIdAndCouponIdToday(user.getId(), coupon.getId())) {
            throw new DailyLimitExceededException("하루에 한 번만 발급 가능한 쿠폰입니다.");
        }
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(CouponResponse::new)
                .collect(Collectors.toList());
    }
}
