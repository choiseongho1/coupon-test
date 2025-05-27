package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponIssue;
import com.coupon.domain.user.User;
import com.coupon.dto.coupon.CouponCreateRequest;
import com.coupon.dto.coupon.CouponIssueResponse;
import com.coupon.exception.CouponAlreadyIssuedException;
import com.coupon.exception.CouponExhaustedException;
import com.coupon.repository.CouponRepository;
import com.coupon.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CouponServiceIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0.0"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));



    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisService redisService;

    private User testUser;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .password("password")
                .name("Test User")
                .build());

        // 테스트용 쿠폰 생성
        CouponCreateRequest request = new CouponCreateRequest(
                "Test Coupon",
                10, // 총 수량 10개
                LocalDateTime.now().minusDays(1), // 1일 전부터
                LocalDateTime.now().plusDays(30) // 30일 후까지
        );
        
        testCoupon = couponRepository.save(Coupon.builder()
                .title(request.getTitle())
                .totalQuantity(request.getTotalQuantity())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build());
                
        // Redis에 쿠폰 재고 초기화
        redisService.initializeCouponStock(testCoupon.getId(), testCoupon.getRemainingQuantity());
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
        
        // Redis 재고 확인
        int remainingStock = redisService.getRemainingCouponStock(testCoupon.getId());
        assertThat(remainingStock).isEqualTo(testCoupon.getRemainingQuantity() - 1);
    }

    @Test
    @DisplayName("중복 쿠폰 발급 방지 테스트")
    void issueCoupon_Duplicate() {
        // given - 첫 번째 쿠폰 발급
        couponService.issueCoupon(testUser.getId(), testCoupon.getId());

        // when & then - 중복 발급 시도
        assertThatThrownBy(() -> couponService.issueCoupon(testUser.getId(), testCoupon.getId()))
                .isInstanceOf(CouponAlreadyIssuedException.class);
    }

    @Test
    @DisplayName("쿠폰 재고 소진 테스트")
    void issueCoupon_OutOfStock() {
        // given - 재고를 1개로 설정
        testCoupon.updateRemainingQuantity(1);
        testCoupon = couponRepository.save(testCoupon);
        redisService.initializeCouponStock(testCoupon.getId(), 1);

        // when - 첫 번째 사용자에게 발급
        couponService.issueCoupon(testUser.getId(), testCoupon.getId());

        // then - 두 번째 사용자에게 발급 시도 시 예외 발생
        User anotherUser = userRepository.save(User.builder()
                .email("another@example.com")
                .password("password")
                .name("Another User")
                .build());

        assertThatThrownBy(() -> couponService.issueCoupon(anotherUser.getId(), testCoupon.getId()))
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    @DisplayName("만료된 쿠폰 발급 방지 테스트")
    void issueCoupon_Expired() {
        // given - 만료된 쿠폰 생성
        Coupon expiredCoupon = couponRepository.save(Coupon.builder()
                .title("Expired Coupon")
                .totalQuantity(10)
                .validFrom(LocalDateTime.now().minusDays(30)) // 30일 전부터
                .validTo(LocalDateTime.now().minusDays(1)) // 어제까지
                .build());

        // when & then - 만료된 쿠폰 발급 시도
        assertThatThrownBy(() -> couponService.issueCoupon(testUser.getId(), expiredCoupon.getId()))
                .isInstanceOf(CouponExhaustedException.class);
    }
    
    @Test
    @DisplayName("동시성 쿠폰 발급 테스트")
    void issueCoupon_Concurrency() throws InterruptedException {
        // given
        int initialStock = 10;
        int threadCount = 20; // 10개 재고에 20개 요청
        
        // 테스트용 쿠폰 생성
        Coupon concurrentCoupon = couponRepository.save(Coupon.builder()
                .title("Concurrent Test Coupon")
                .totalQuantity(initialStock)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build());
        
        // 동시성 테스트를 위한 설정
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        // when: 동시에 여러 사용자가 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    // 테스트용 사용자 생성
                    User user = userRepository.save(User.builder()
                            .email("concurrent" + userIndex + "@example.com")
                            .password("password")
                            .name("Concurrent User " + userIndex)
                            .build());
                    
                    try {
                        // 쿠폰 발급 시도
                        couponService.issueCoupon(user.getId(), concurrentCoupon.getId());
                        successCount.incrementAndGet();
                    } catch (CouponExhaustedException e) {
                        // 재고 부족 예외
                        failCount.incrementAndGet();
                    } catch (CouponAlreadyIssuedException e) {
                        // 이미 발급됨 예외 (일어나지 않아야 함)
                        org.junit.jupiter.api.Assertions.fail("Unexpected CouponAlreadyIssuedException");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드 완료 대기
        latch.await(20, TimeUnit.SECONDS);
        executorService.shutdown();
        
        // then
        // 쿠폰 재조회
        Coupon updatedCoupon = couponRepository.findById(concurrentCoupon.getId()).orElseThrow();
        
        // 검증: 성공 + 실패 = 총 요청 수
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        
        // 검증: 성공한 발급 수 = 초기 재고 (최대 10개까지만 성공 가능)
        assertThat(successCount.get()).isEqualTo(initialStock);
        
        // 검증: 남은 재고 = 0 (모두 소진되어야 함)
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(0);
        
        // 로그 출력
        System.out.println("성공: " + successCount.get() + ", 실패: " + failCount.get());
    }
}
