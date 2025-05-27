package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
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

    /**
     * 매일 자정에 만료된 쿠폰 처리
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
                // 여기서 만료 처리 로직을 추가할 수 있습니다.
                // 예: 알림 전송, 로깅 등
            }
        } else {
            log.info("No expired coupons found");
        }
    }
}
