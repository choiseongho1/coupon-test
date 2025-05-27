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
import com.coupon.exception.InternalServerException;
import com.coupon.repository.CouponIssueRepository;
import com.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final UserService userService;
    private final RedisService redisService;

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        Coupon coupon = Coupon.builder()
                .title(request.getTitle())
                .totalQuantity(request.getTotalQuantity())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        
        // Redis에 쿠폰 재고 초기화
        redisService.initializeCouponStock(savedCoupon.getId(), savedCoupon.getTotalQuantity());
        
        return new CouponResponse(savedCoupon);
    }

    @Transactional
    public CouponIssueResponse issueCoupon(Long userId, Long couponId) {
        log.info("Attempting to issue coupon - userId: {}, couponId: {}", userId, couponId);
        
        try {
            // 1. 사용자 조회
            User user = userService.findById(userId);
            
            // 2. Redis를 통한 쿠폰 발급 시도 (분산 락 대체)
            Long result = redisService.tryIssueCoupon(couponId, userId);
            
            // 3. 발급 결과 처리
            if (result == null) {
                log.error("Unexpected null result from Redis for userId: {}, couponId: {}", userId, couponId);
                throw new InternalServerException("쿠폰 발급 처리 중 오류가 발생했습니다.");
            } else if (result == 0) {
                log.info("Coupon already issued - userId: {}, couponId: {}", userId, couponId);
                throw new CouponAlreadyIssuedException("이미 발급받은 쿠폰입니다.");
            } else if (result < 0) {
                log.info("Coupon out of stock - userId: {}, couponId: {}", userId, couponId);
                throw new CouponExhaustedException("쿠폰이 모두 소진되었습니다.");
            } else if (result != 1) {
                log.error("Unexpected result from Redis: {} for userId: {}, couponId: {}", 
                        result, userId, couponId);
                throw new InternalServerException("쿠폰 발급 중 예상치 못한 오류가 발생했습니다.");
            }
            
            // 4. 쿠폰 조회 (비관적 락 적용)
            Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
                    .orElseThrow(() -> {
                        log.error("Coupon not found after successful Redis operation - couponId: {}", couponId);
                        return new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + couponId);
                    });
                    
            // 5. 쿠폰 유효성 검사 (기간 등)
            validateCouponIssue(user, coupon);
            
            // 6. DB에 쿠폰 발급 이력 저장
            CouponIssue couponIssue = CouponIssue.builder()
                    .user(user)
                    .coupon(coupon)
                    .issuedAt(LocalDateTime.now())
                    .build();
            
            CouponIssue savedCouponIssue = couponIssueRepository.save(couponIssue);
            
            // 7. 재고 동기화 (Redis와 DB 간의 불일치 방지를 위한 안전장치)
            int remainingInRedis = redisService.getRemainingCouponStock(couponId);
            int expectedRemaining = coupon.getRemainingQuantity() - 1;
            
            if (remainingInRedis != expectedRemaining) {
                log.warn("Redis and DB stock mismatch detected - couponId: {}, Redis: {}, DB: {}", 
                        couponId, remainingInRedis, expectedRemaining);
                
                // Redis 재고를 DB와 동기화 (안전장치)
                try {
                    redisService.initializeCouponStock(couponId, expectedRemaining);
                    log.info("Synchronized Redis stock with DB - couponId: {}, new stock: {}", 
                            couponId, expectedRemaining);
                } catch (Exception e) {
                    log.error("Failed to synchronize Redis stock with DB - couponId: {}", couponId, e);
                    // 계속 진행 (주요 비즈니스 로직에는 영향을 주지 않음)
                }
            }
            
            log.info("Successfully issued coupon - userId: {}, couponId: {}, issueId: {}", 
                    userId, couponId, savedCouponIssue.getId());
                    
            return new CouponIssueResponse(
                    savedCouponIssue.getId(),
                    savedCouponIssue.getUser().getId(),
                    savedCouponIssue.getCoupon().getId(),
                    savedCouponIssue.getIssuedAt()
            );
            
        } catch (CouponAlreadyIssuedException | CouponExhaustedException | CouponExpiredException e) {
            // 이미 처리된 비즈니스 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while issuing coupon - userId: {}, couponId: {}", userId, couponId, e);
            throw new InternalServerException("쿠폰 발급 처리 중 오류가 발생했습니다.", e);
        }
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
