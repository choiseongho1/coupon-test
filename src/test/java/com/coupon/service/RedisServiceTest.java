package com.coupon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisServiceTest {

    @InjectMocks
    private RedisService redisService;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private SetOperations<String, String> setOperations;

    @BeforeEach
    void setUp() {
        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("쿠폰 재고 초기화 및 조회 테스트")
    void testInitializeAndGetCouponStock() {
        // given
        Long couponId = 1L;
        int initialQuantity = 100;
        String stockKey = "coupon:" + couponId;
        
        // when
        when(valueOperations.get(stockKey)).thenReturn(String.valueOf(initialQuantity));
        
        redisService.initializeCouponStock(couponId, initialQuantity);
        int remainingStock = redisService.getRemainingCouponStock(couponId);

        // then
        assertThat(remainingStock).isEqualTo(initialQuantity);
    }

    @Test
    @DisplayName("쿠폰 발급 성공 테스트")
    void testTryIssueCoupon_Success() {
        // given
        Long couponId = 2L;
        Long userId = 1L;
        int initialQuantity = 10;
        String stockKey = "coupon:" + couponId;
        String userKey = "coupon:user:" + userId;
        String lockKey = "lock:coupon:" + couponId;
        
        // Mock lock acquisition and release
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.delete(lockKey)).thenReturn(true);
        
        // Mock user set check
        when(setOperations.isMember(userKey, couponId.toString())).thenReturn(false);
        
        // Mock stock check and decrement
        when(valueOperations.get(stockKey)).thenReturn(String.valueOf(initialQuantity));
        when(valueOperations.increment(stockKey, -1)).thenReturn((long) (initialQuantity - 1));
        
        // Mock user set add
        when(setOperations.add(userKey, couponId.toString())).thenReturn(1L);

        // when
        Long result = redisService.tryIssueCoupon(couponId, userId);
        
        // Mock remaining stock for second call
        when(valueOperations.get(stockKey)).thenReturn(String.valueOf(initialQuantity - 1));
        int remainingStock = redisService.getRemainingCouponStock(couponId);

        // then
        assertThat(result).isEqualTo(1L); // 성공
        assertThat(remainingStock).isEqualTo(initialQuantity - 1);
    }

    @Test
    @DisplayName("중복 발급 방지 테스트")
    void testTryIssueCoupon_Duplicate() {
        // given
        Long couponId = 3L;
        Long userId = 1L;
        String stockKey = "coupon:" + couponId;
        String userKey = "coupon:user:" + userId;
        String lockKey = "lock:coupon:" + couponId;
        
        // Mock lock acquisition and release
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.delete(lockKey)).thenReturn(true);
        
        // Mock user set check - 이미 발급됨 상태 모킹
        when(setOperations.isMember(userKey, couponId.toString())).thenReturn(true);
        
        // when: 동일 사용자가 같은 쿠폰을 다시 발급 시도
        Long result = redisService.tryIssueCoupon(couponId, userId);
        
        // then
        assertThat(result).isEqualTo(0L); // 이미 발급됨
    }
    
    @Test
    @DisplayName("재고 부족 테스트")
    void testTryIssueCoupon_OutOfStock() {
        // given
        Long couponId = 4L;
        Long userId = 2L; // 다른 사용자
        String stockKey = "coupon:" + couponId;
        String userKey = "coupon:user:" + userId;
        String lockKey = "lock:coupon:" + couponId;
        
        // Mock lock acquisition and release
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.delete(lockKey)).thenReturn(true);
        
        // Mock user set check - 사용자는 아직 발급받지 않음
        when(setOperations.isMember(userKey, couponId.toString())).thenReturn(false);
        
        // Mock stock check - 재고가 0개
        when(valueOperations.get(stockKey)).thenReturn("0");
        
        // when: 재고가 없는 상태에서 발급 시도
        Long result = redisService.tryIssueCoupon(couponId, userId);
        
        // then
        assertThat(result).isEqualTo(-1L); // 재고 부족
    }
    
    @Test
    @DisplayName("동시 요청 처리 테스트 - 모킹 버전")
    void testConcurrentRequests() throws InterruptedException {
        // given
        Long couponId = 5L;
        Long userId1 = 1L;
        Long userId2 = 2L;
        String stockKey = "coupon:" + couponId;
        String userKey1 = "coupon:user:" + userId1;
        String userKey2 = "coupon:user:" + userId2;
        String lockKey = "lock:coupon:" + couponId;
        
        // 첫 번째 사용자 - 성공 케이스 모킹
        // Mock lock acquisition and release
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), any(Duration.class)))
            .thenReturn(true);
        when(redisTemplate.delete(lockKey)).thenReturn(true);
        
        // Mock user set check - 첫 번째 사용자는 발급받지 않음
        when(setOperations.isMember(userKey1, couponId.toString())).thenReturn(false);
        
        // Mock stock check - 재고가 1개
        when(valueOperations.get(stockKey)).thenReturn("1");
        
        // Mock stock decrement
        when(valueOperations.increment(stockKey, -1)).thenReturn(0L);
        
        // Mock user set add
        when(setOperations.add(userKey1, couponId.toString())).thenReturn(1L);
        
        // 첫 번째 사용자 요청 - 성공
        Long result1 = redisService.tryIssueCoupon(couponId, userId1);
        
        // 두 번째 사용자 - 재고 부족 케이스 모킹
        // 재고가 0개로 변경
        when(valueOperations.get(stockKey)).thenReturn("0");
        when(setOperations.isMember(userKey2, couponId.toString())).thenReturn(false);
        
        // 두 번째 사용자 요청 - 실패 (재고 부족)
        Long result2 = redisService.tryIssueCoupon(couponId, userId2);
        
        // then
        assertThat(result1).isEqualTo(1L); // 성공
        assertThat(result2).isEqualTo(-1L); // 재고 부족
    }
}
