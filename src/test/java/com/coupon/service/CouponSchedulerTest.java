package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponStatus;
import com.coupon.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponSchedulerTest {

    @Mock
    private CouponRepository couponRepository;
    
    @Mock
    private RedisService redisService;
    
    @InjectMocks
    private CouponScheduler couponScheduler;
    
    private Coupon expiredCoupon1;
    private Coupon expiredCoupon2;
    
    @BeforeEach
    void setUp() {
        // RedisService 메소드 스터빙
        doNothing().when(redisService).initializeCouponStock(anyLong(), anyInt());
        // 만료된 쿠폰 1
        expiredCoupon1 = Coupon.builder()
                .title("만료된 쿠폰 1")
                .totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(30))
                .validTo(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(expiredCoupon1, "id", 1L);
        ReflectionTestUtils.setField(expiredCoupon1, "remainingQuantity", 50);
        
        // 만료된 쿠폰 2
        expiredCoupon2 = Coupon.builder()
                .title("만료된 쿠폰 2")
                .totalQuantity(200)
                .validFrom(LocalDateTime.now().minusDays(60))
                .validTo(LocalDateTime.now().minusDays(2))
                .status(CouponStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(expiredCoupon2, "id", 2L);
        ReflectionTestUtils.setField(expiredCoupon2, "remainingQuantity", 100);
    }
    
    @Test
    @DisplayName("만료된 쿠폰 처리 성공")
    void processExpiredCoupons_Success() {
        // given
        List<Coupon> expiredCoupons = Arrays.asList(expiredCoupon1, expiredCoupon2);
        given(couponRepository.findByValidToBeforeAndRemainingQuantityGreaterThan(any(LocalDateTime.class), eq(0)))
                .willReturn(expiredCoupons);
        
        // when
        couponScheduler.processExpiredCoupons();
        
        // then
        verify(couponRepository).findByValidToBeforeAndRemainingQuantityGreaterThan(any(LocalDateTime.class), eq(0));
        verify(couponRepository, times(2)).save(any(Coupon.class));
        // 각 쿠폰의 상태가 EXPIRED로 변경되었는지 확인
        
        // 쿠폰 상태 및 재고 확인
        verify(couponRepository).save(expiredCoupon1);
        verify(couponRepository).save(expiredCoupon2);
        verify(redisService, times(1)).initializeCouponStock(eq(1L), eq(0));
        verify(redisService, times(1)).initializeCouponStock(eq(2L), eq(0));
    }
    
    @Test
    @DisplayName("만료된 쿠폰이 없는 경우")
    void processExpiredCoupons_NoCouponsExpired() {
        // given
        given(couponRepository.findByValidToBeforeAndRemainingQuantityGreaterThan(any(LocalDateTime.class), eq(0)))
                .willReturn(List.of());
        
        // when
        couponScheduler.processExpiredCoupons();
        
        // then
        verify(couponRepository).findByValidToBeforeAndRemainingQuantityGreaterThan(any(LocalDateTime.class), eq(0));
        verify(couponRepository, times(0)).save(any(Coupon.class));
        verify(redisService, times(0)).initializeCouponStock(anyLong(), any(Integer.class));
    }
}
