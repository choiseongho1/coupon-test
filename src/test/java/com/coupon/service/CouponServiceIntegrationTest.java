package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponIssue;
import com.coupon.domain.coupon.CouponStatus;
import com.coupon.domain.user.User;
import com.coupon.dto.coupon.CouponCreateRequest;
import com.coupon.dto.coupon.CouponIssueResponse;
import com.coupon.exception.CouponAlreadyIssuedException;
import com.coupon.exception.CouponExhaustedException;
import com.coupon.exception.CouponExpiredException;
import com.coupon.exception.DailyLimitExceededException;
import com.coupon.exception.InternalServerException;
import com.coupon.repository.CouponIssueRepository;
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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceIntegrationTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponIssueRepository couponIssueRepository;

    @Mock
    private UserService userService;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private CouponService couponService;

    private User testUser;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // Initialize test data with ReflectionTestUtils to set ID fields
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .name("Test User")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testCoupon = Coupon.builder()
                .title("Test Coupon")
                .totalQuantity(10)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(testCoupon, "id", 1L);
        ReflectionTestUtils.setField(testCoupon, "remainingQuantity", 10);

        // Mock service and repository responses
        lenient().when(userService.findById(anyLong())).thenReturn(testUser);
        lenient().when(couponRepository.findByIdWithPessimisticLock(anyLong())).thenReturn(Optional.of(testCoupon));
        lenient().when(couponIssueRepository.existsByUserIdAndCouponId(anyLong(), anyLong())).thenReturn(false);
        lenient().when(couponIssueRepository.existsByUserIdAndCouponIdToday(anyLong(), anyLong())).thenReturn(false);
        lenient().when(redisService.tryIssueCoupon(anyLong(), anyLong())).thenReturn(1L);
        lenient().when(redisService.getRemainingCouponStock(anyLong())).thenReturn(9); // After issuing, 9 remain
        
        // Mock CouponIssue save
        lenient().when(couponIssueRepository.save(any(CouponIssue.class))).thenAnswer(invocation -> {
            CouponIssue couponIssue = invocation.getArgument(0);
            ReflectionTestUtils.setField(couponIssue, "id", 1L);
            return couponIssue;
        });
    }

    @Test
    @DisplayName("쿠폰 정상 발급 테스트")
    void issueCoupon_Success() {
        // when
        CouponIssueResponse response = couponService.issueCoupon(testUser.getId(), testCoupon.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getCouponId()).isEqualTo(testCoupon.getId());
        assertThat(response.getIssuedAt()).isNotNull();
        
        // Verify interactions
        verify(userService).findById(testUser.getId());
        verify(couponRepository).findByIdWithPessimisticLock(testCoupon.getId());
        verify(couponIssueRepository).existsByUserIdAndCouponId(testUser.getId(), testCoupon.getId());
        verify(couponIssueRepository).existsByUserIdAndCouponIdToday(testUser.getId(), testCoupon.getId());
        verify(redisService).tryIssueCoupon(testCoupon.getId(), testUser.getId());
        verify(couponIssueRepository).save(any(CouponIssue.class));
    }

    @Test
    @DisplayName("중복 쿠폰 발급 방지 테스트")
    void issueCoupon_Duplicate() {
        // given
        given(userService.findById(1L)).willReturn(testUser);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(testCoupon));
        given(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(testUser.getId(), testCoupon.getId()))
                .isInstanceOf(CouponAlreadyIssuedException.class)
                .hasMessage("이미 발급받은 쿠폰입니다.");

        verify(couponIssueRepository, never()).save(any(CouponIssue.class));
    }

    @Test
    @DisplayName("쿠폰 재고 소진 테스트")
    void issueCoupon_OutOfStock() {
        // given
        given(userService.findById(1L)).willReturn(testUser);
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(-1L); // -1 indicates out of stock

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(testUser.getId(), testCoupon.getId()))
                .isInstanceOf(CouponExhaustedException.class)
                .hasMessage("쿠폰이 모두 소진되었습니다.");
        
        // Verify that repository save was never called
        verify(couponIssueRepository, never()).save(any(CouponIssue.class));
    }

    @Test
    @DisplayName("만료된 쿠폰 발급 방지 테스트")
    void issueCoupon_Expired() {
        // given
        Coupon expiredCoupon = Coupon.builder()
                .title("Expired Coupon")
                .totalQuantity(10)
                .validFrom(LocalDateTime.now().minusDays(30))
                .validTo(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(expiredCoupon, "id", 2L);
        ReflectionTestUtils.setField(expiredCoupon, "remainingQuantity", 10);

        given(userService.findById(1L)).willReturn(testUser);
        given(redisService.tryIssueCoupon(2L, 1L)).willReturn(1L); // Success in Redis
        given(couponRepository.findByIdWithPessimisticLock(2L)).willReturn(Optional.of(expiredCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(testUser.getId(), expiredCoupon.getId()))
                .isInstanceOf(CouponExpiredException.class)
                .hasMessage("만료된 쿠폰입니다.");
                
        // Verify that repository save was never called
        verify(couponIssueRepository, never()).save(any(CouponIssue.class));
    }
    
    @Test
    @DisplayName("하루 발급 횟수 초과 테스트")
    void issueCoupon_DailyLimitExceeded() {
        // given
        given(userService.findById(1L)).willReturn(testUser);
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(1L); // Success in Redis
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(testCoupon));
        given(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);
        given(couponIssueRepository.existsByUserIdAndCouponIdToday(1L, 1L)).willReturn(true); // Already issued today

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(testUser.getId(), testCoupon.getId()))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessage("하루에 한 번만 발급 가능한 쿠폰입니다.");
                
        // Verify that repository save was never called
        verify(couponIssueRepository, never()).save(any(CouponIssue.class));
    }

    @Test
    @DisplayName("동시성 쿠폰 발급 테스트")
    void issueCoupon_Concurrency() throws InterruptedException {
        // given
        int threadCount = 20;
        int successCount = 10; // Only 10 should succeed
        
        // Reset common mocks
        reset(redisService);
        reset(couponRepository);
        reset(couponIssueRepository);
        
        // Create a counter to track how many times tryIssueCoupon is called
        final AtomicInteger counter = new AtomicInteger(0);
        
        // Mock RedisService to allow only 'successCount' successful issues
        when(redisService.tryIssueCoupon(anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    int currentCount = counter.incrementAndGet();
                    if (currentCount <= successCount) {
                        return 1L; // Success
                    } else {
                        return -1L; // Out of stock
                    }
                });
        
        // Mock other necessary dependencies
        when(userService.findById(anyLong())).thenReturn(testUser);
        when(couponRepository.findByIdWithPessimisticLock(anyLong())).thenReturn(Optional.of(testCoupon));
        when(couponIssueRepository.existsByUserIdAndCouponId(anyLong(), anyLong())).thenReturn(false);
        when(couponIssueRepository.existsByUserIdAndCouponIdToday(anyLong(), anyLong())).thenReturn(false);
        when(redisService.getRemainingCouponStock(anyLong())).thenReturn(0);
        when(couponIssueRepository.save(any(CouponIssue.class))).thenAnswer(invocation -> {
            CouponIssue issue = invocation.getArgument(0);
            ReflectionTestUtils.setField(issue, "id", 1L);
            return issue;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger actualSuccessCount = new AtomicInteger(0);
        AtomicInteger actualFailCount = new AtomicInteger(0);

        // when: Simulate concurrent requests
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(userId, testCoupon.getId());
                    actualSuccessCount.incrementAndGet();
                } catch (CouponExhaustedException e) {
                    actualFailCount.incrementAndGet();
                } catch (Exception e) {
                    // Log other exceptions but still count as failures
                    System.err.println("Unexpected exception: " + e.getMessage());
                    actualFailCount.incrementAndGet();
                } finally {
                    latch.countDown(); // 이 부분이 누락되었습니다
                }
            });
        }
        
        // Wait for all threads to complete (with timeout)
        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();
        
        // then
        assertThat(actualSuccessCount.get()).isEqualTo(successCount);
        assertThat(actualFailCount.get()).isEqualTo(threadCount - successCount);
    }
}