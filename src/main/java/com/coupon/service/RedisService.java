package com.coupon.service;

import com.coupon.exception.RedisOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Redis 서비스를 제공하는 클래스입니다.
 * 쿠폰 발급, 재고 관리 등의 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private static final String COUPON_KEY_PREFIX = "coupon:";
    private static final String USER_KEY_PREFIX = "coupon:user:";
    private static final String LOCK_KEY_PREFIX = "lock:coupon:";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration USER_COUPON_EXPIRY = Duration.ofDays(30);
    
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 쿠폰 재고를 초기화합니다.
     * @param couponId 쿠폰 ID
     * @param quantity 초기 수량
     * @throws RedisOperationException Redis 작업 중 오류가 발생한 경우
     */
    public void initializeCouponStock(Long couponId, int quantity) {
        try {
            if (couponId == null || quantity < 0) {
                throw new IllegalArgumentException("Invalid couponId or quantity");
            }
            
            String key = getCouponKey(couponId);
            redisTemplate.opsForValue().set(key, String.valueOf(quantity));
            log.info("Initialized coupon stock - couponId: {}, quantity: {}", couponId, quantity);
        } catch (Exception e) {
            log.error("Failed to initialize coupon stock. couponId: {}, quantity: {}", couponId, quantity, e);
            throw new RedisOperationException("Failed to initialize coupon stock", e);
        }
    }

    /**
     * 쿠폰 발급을 시도합니다. 분산 락을 사용하여 동시성을 제어합니다.
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 결과 (1: 성공, 0: 이미 발급됨, -1: 재고 부족, -2: 락 획득 실패)
     * @throws RedisOperationException Redis 작업 중 오류가 발생한 경우
     */
    public Long tryIssueCoupon(Long couponId, Long userId) {
        if (couponId == null || userId == null) {
            throw new IllegalArgumentException("couponId and userId must not be null");
        }
        
        String couponKey = getCouponKey(couponId);
        String userKey = getUserKey(userId);
        String lockKey = LOCK_KEY_PREFIX + couponId;
        
        log.debug("Attempting to issue coupon - couponId: {}, userId: {}", couponId, userId);
        
        try {
            // 1. 락 획득 시도 (일정 시간 동안만 유효)
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, userId.toString(), LOCK_TIMEOUT);
            if (!Boolean.TRUE.equals(acquired)) {
                log.debug("Failed to acquire lock for couponId: {}", couponId);
                return -2L; // 락 획득 실패
            }
            
            try {
                // 2. 이미 발급받은 쿠폰인지 확인
                Boolean isMember = redisTemplate.opsForSet().isMember(userKey, couponId.toString());
                if (Boolean.TRUE.equals(isMember)) {
                    log.debug("User already has this coupon - couponId: {}, userId: {}", couponId, userId);
                    return 0L; // 이미 발급됨
                }
                
                // 3. 재고 확인
                String remainingStr = redisTemplate.opsForValue().get(couponKey);
                int remaining = remainingStr != null ? Integer.parseInt(remainingStr) : 0;
                if (remaining <= 0) {
                    log.debug("Coupon out of stock - couponId: {}", couponId);
                    return -1L; // 재고 부족
                }
                
                // 4. 재고 감소
                redisTemplate.opsForValue().decrement(couponKey);
                
                // 5. 사용자에게 쿠폰 발급 (사용자 집합에 쿠폰 ID 추가)
                redisTemplate.opsForSet().add(userKey, couponId.toString());
                
                // 6. 사용자 쿠폰 집합에 만료시간 설정 (30일)
                redisTemplate.expire(userKey, USER_COUPON_EXPIRY);
                
                log.debug("Successfully issued coupon - couponId: {}, userId: {}", couponId, userId);
                return 1L; // 성공
            } finally {
                // 7. 락 해제
                redisTemplate.delete(lockKey);
                log.debug("Released lock for couponId: {}", couponId);
            }
        } catch (Exception e) {
            log.error("Error while trying to issue coupon - couponId: {}, userId: {}", couponId, userId, e);
            throw new RedisOperationException("Failed to issue coupon", e);
        }
    }
    
    /**
     * 발급 결과 코드에 대한 설명
     * 1: 성공
     * 0: 이미 발급됨
     * -1: 재고 부족
     * -2: 락 획득 실패
     */

    /**
     * 쿠폰 재고를 조회합니다.
     * @param couponId 쿠폰 ID
     * @return 남은 재고 수량 (없으면 0 반환)
     * @throws RedisOperationException Redis 작업 중 오류가 발생한 경우
     */
    public int getRemainingCouponStock(Long couponId) {
        if (couponId == null) {
            throw new IllegalArgumentException("couponId must not be null");
        }
        
        try {
            String key = getCouponKey(couponId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.debug("No stock information found for coupon: {}", couponId);
                return 0;
            }
            
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.error("Invalid stock value in Redis for coupon: {}, value: {}", couponId, value, e);
                throw new RedisOperationException("Invalid stock value in Redis", e);
            }
        } catch (Exception e) {
            log.error("Failed to get remaining coupon stock. couponId: {}", couponId, e);
            throw new RedisOperationException("Failed to get remaining coupon stock", e);
        }
    }

    /**
     * 쿠폰 발급 가능 여부를 확인합니다.
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 가능 여부 (true: 발급 가능, false: 이미 발급됨)
     * @throws RedisOperationException Redis 작업 중 오류가 발생한 경우
     */
    public boolean canIssueCoupon(Long couponId, Long userId) {
        if (couponId == null || userId == null) {
            throw new IllegalArgumentException("couponId and userId must not be null");
        }
        
        try {
            // 1. Check if user already has this coupon
            String userKey = getUserKey(userId);
            Boolean isMember = redisTemplate.opsForSet().isMember(userKey, String.valueOf(userId));
            if (Boolean.TRUE.equals(isMember)) {
                log.debug("User already has this coupon - couponId: {}, userId: {}", couponId, userId);
                return false;
            }
            
            // 2. Check if coupon has remaining stock
            int remainingStock = getRemainingCouponStock(couponId);
            boolean hasStock = remainingStock > 0;
            
            log.debug("Checking if coupon can be issued - couponId: {}, userId: {}, hasStock: {}", 
                    couponId, userId, hasStock);
                    
            return hasStock;
        } catch (Exception e) {
            log.error("Failed to check if coupon can be issued. couponId: {}, userId: {}", couponId, userId, e);
            throw new RedisOperationException("Failed to check if coupon can be issued", e);
        }
    }

    private String getCouponKey(Long couponId) {
        if (couponId == null) {
            throw new IllegalArgumentException("couponId must not be null");
        }
        return COUPON_KEY_PREFIX + couponId;
    }

    private String getUserKey(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return USER_KEY_PREFIX + userId;
    }
}
