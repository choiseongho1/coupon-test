package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponStatus;
import com.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponScheduler {

    private final CouponRepository couponRepository;
    private final RedisService redisService;

    /**
     * 매일 자정에 만료된 쿠폰을 처리합니다.
     * 현재 시간을 기준으로 유효기간이 만료되었지만 재고가 남아있는 쿠폰을 처리합니다.
     * 만료된 쿠폰을 검색하고 로깅하며, 추가적인 만료 처리 로직을 구현할 수 있습니다.
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    @Transactional
    public void processExpiredCoupons() {
        log.info("Processing expired coupons at: {}", LocalDateTime.now());
        
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> expiredCoupons = couponRepository.findByValidToBeforeAndRemainingQuantityGreaterThan(now, 0);
        
        if (!expiredCoupons.isEmpty()) {
            log.info("Found {} expired coupons", expiredCoupons.size());
            
            for (Coupon coupon : expiredCoupons) {
                log.info("Expiring coupon: {} (ID: {})", coupon.getTitle(), coupon.getId());
                
                // 1. 쿠폰 상태를 EXPIRED로 변경
                coupon.setStatus(CouponStatus.EXPIRED);
                
                // 2. 남은 수량을 0으로 설정
                int oldQuantity = coupon.getRemainingQuantity();
                coupon.updateRemainingQuantity(0);
                
                // 3. DB에 변경사항 저장
                couponRepository.save(coupon);
                
                // 4. Redis의 재고 정보도 0으로 업데이트
                try {
                    redisService.initializeCouponStock(coupon.getId(), 0);
                    log.info("Updated Redis stock for expired coupon: {} (ID: {}), old quantity: {}", 
                            coupon.getTitle(), coupon.getId(), oldQuantity);
                } catch (Exception e) {
                    log.error("Failed to update Redis stock for expired coupon: {} (ID: {})", 
                            coupon.getTitle(), coupon.getId(), e);
                    // Redis 업데이트 실패는 트랜잭션을 롤백하지 않음 (DB 업데이트가 우선)
                }
            }
        } else {
            log.info("No expired coupons found");
        }
    }
}
