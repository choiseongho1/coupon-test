package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponIssue;
import com.coupon.domain.coupon.CouponStatus;
import com.coupon.domain.user.User;
import com.coupon.dto.coupon.CouponCreateRequest;
import com.coupon.dto.coupon.CouponIssueResponse;
import com.coupon.dto.coupon.CouponResponse;
import com.coupon.dto.coupon.CouponStatisticsResponse;
import com.coupon.exception.*;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserService userService;

    @Mock
    private RedisService redisService;

    @Mock
    private CouponIssueRepository couponIssueRepository;

    @InjectMocks
    private CouponService couponService;

    private User user;
    private Coupon coupon;
    private CouponCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        // RedisService 메소드 스터빙
        lenient().doNothing().when(redisService).initializeCouponStock(anyLong(), anyInt());
        lenient().when(redisService.tryIssueCoupon(anyLong(), anyLong())).thenReturn(1L);
        user = User.builder()
                .email("test@example.com")
                .name("테스트사용자")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        coupon = Coupon.builder()
                .title("테스트 쿠폰")
                .totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(coupon, "id", 1L);
        ReflectionTestUtils.setField(coupon, "remainingQuantity", 100);

        createRequest = new CouponCreateRequest("테스트 쿠폰", 100, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("쿠폰 생성 성공")
    void createCoupon_Success() {
        // given
        doNothing().when(redisService).initializeCouponStock(anyLong(), anyInt());
        given(couponRepository.save(any(Coupon.class))).willReturn(coupon);

        // when
        CouponResponse response = couponService.createCoupon(createRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo(createRequest.getTitle());
        assertThat(response.getTotalQuantity()).isEqualTo(createRequest.getTotalQuantity());
        
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 사용자 없음")
    void issueCoupon_UserNotFound() {
        // given
        given(userService.findById(999L)).willThrow(new InternalServerException("쿠폰 발급 처리 중 오류가 발생했습니다."));
        // These stubs are not used in this test path, so we make them lenient
        lenient().when(redisService.tryIssueCoupon(anyLong(), anyLong())).thenReturn(1L);
        lenient().when(couponRepository.findByIdWithPessimisticLock(anyLong())).thenReturn(Optional.of(coupon));
        lenient().when(couponIssueRepository.existsByUserIdAndCouponId(anyLong(), anyLong())).thenReturn(false);
        lenient().when(couponIssueRepository.existsByUserIdAndCouponIdToday(anyLong(), anyLong())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(999L, 1L))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("쿠폰 발급 처리 중 오류가 발생했습니다.");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 없음")
    void issueCoupon_CouponNotFound() {
        // given
        given(userService.findById(1L)).willReturn(user);
        given(redisService.tryIssueCoupon(999L, 1L)).willReturn(1L);
        given(couponRepository.findByIdWithPessimisticLock(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 999L))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("쿠폰 발급 처리 중 오류가 발생했습니다");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {
        // given
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));
        given(userService.findById(1L)).willReturn(user);
        given(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);
        given(couponIssueRepository.existsByUserIdAndCouponIdToday(1L, 1L)).willReturn(false);
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(1L);
        
        // CouponIssue 저장 모킹
        CouponIssue couponIssue = CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .issuedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(couponIssue, "id", 1L);
        given(couponIssueRepository.save(any(CouponIssue.class))).willReturn(couponIssue);

        // when
        CouponIssueResponse response = couponService.issueCoupon(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCouponId()).isEqualTo(1L);
        assertThat(response.getIssuedAt()).isNotNull();
        
        verify(couponIssueRepository).save(any(CouponIssue.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 소진된 쿠폰")
    void issueCoupon_CouponExhausted() {
        // given
        // Set up the user and coupon
        given(userService.findById(1L)).willReturn(user);
        
        // Mock Redis to return -1, indicating the coupon is exhausted
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(-1L);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponExhaustedException.class)
                .hasMessageContaining("쿠폰이 모두 소진되었습니다");
                
        // Verify that the repository save methods were never called
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
        
        // Verify that existsByUserIdAndCouponId and existsByUserIdAndCouponIdToday were never called
        verify(couponIssueRepository, never()).existsByUserIdAndCouponId(anyLong(), anyLong());
        verify(couponIssueRepository, never()).existsByUserIdAndCouponIdToday(anyLong(), anyLong());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 만료된 쿠폰")
    void issueCoupon_CouponExpired() {
        // given
        coupon = Coupon.builder()
                .title("만료된 쿠폰")
                .totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(30))
                .validTo(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.EXPIRED)
                .build();
        ReflectionTestUtils.setField(coupon, "id", 1L);
        ReflectionTestUtils.setField(coupon, "remainingQuantity", 100);
        
        given(userService.findById(1L)).willReturn(user);
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(1L);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));
        // These stubs might not be called in this test path, so make them lenient
        lenient().when(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false);
        lenient().when(couponIssueRepository.existsByUserIdAndCouponIdToday(1L, 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponExpiredException.class)
                .hasMessageContaining("만료된 쿠폰입니다");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 일일 발급 한도 초과")
    void issueCoupon_DailyLimitExceeded() {
        // given
        given(userService.findById(1L)).willReturn(user);
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(1L);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);
        // 일일 발급 한도 초과 설정
        given(couponIssueRepository.existsByUserIdAndCouponIdToday(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessageContaining("하루에 한 번만 발급 가능한 쿠폰입니다");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급받은 쿠폰")
    void issueCoupon_AlreadyIssued() {
        // given
        given(userService.findById(1L)).willReturn(user);
        // Redis가 0을 반환하면 이미 발급받은 쿠폰임을 의미
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(0L);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponAlreadyIssuedException.class)
                .hasMessageContaining("이미 발급받은 쿠폰입니다");
                
        verify(couponRepository, never()).findByIdWithPessimisticLock(anyLong());
        verify(couponIssueRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("쿠폰 발급 실패 - Redis 결과값 null")
    void issueCoupon_RedisResultNull() {
        // given
        given(userService.findById(1L)).willReturn(user);
        // Redis가 null을 반환하는 경우
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("쿠폰 발급 처리 중 오류가 발생했습니다");
                
        verify(couponRepository, never()).findByIdWithPessimisticLock(anyLong());
        verify(couponIssueRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("쿠폰 발급 실패 - Redis 예상치 못한 결과값")
    void issueCoupon_RedisUnexpectedResult() {
        // given
        given(userService.findById(1L)).willReturn(user);
        // Redis가 예상치 못한 값(2)을 반환하는 경우
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(2L);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("쿠폰 발급 처리 중 오류가 발생했습니다");
                
        verify(couponRepository, never()).findByIdWithPessimisticLock(anyLong());
        verify(couponIssueRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("쿠폰 발급 성공 - Redis-DB 동기화 테스트")
    void issueCoupon_SuccessWithSynchronization() {
        // given
        given(userService.findById(1L)).willReturn(user);
        given(redisService.tryIssueCoupon(1L, 1L)).willReturn(1L);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);
        given(couponIssueRepository.existsByUserIdAndCouponIdToday(1L, 1L)).willReturn(false);
        
        // CouponIssue 저장 모킹
        CouponIssue couponIssue = CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .issuedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(couponIssue, "id", 1L);
        given(couponIssueRepository.save(any(CouponIssue.class))).willReturn(couponIssue);
        
        // Redis와 DB 재고 불일치 시나리오
        given(redisService.getRemainingCouponStock(1L)).willReturn(98); // Redis에는 98개
        // DB에는 99개가 남았을 것으로 가정 (100-1)

        // when
        CouponIssueResponse response = couponService.issueCoupon(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCouponId()).isEqualTo(1L);
        
        // Redis-DB 동기화 확인
        verify(redisService).initializeCouponStock(eq(1L), eq(99));
    }
    
    @Test
    @DisplayName("내 발급 쿠폰 조회 성공")
    void getIssuedCoupons_Success() {
        // given
        CouponIssue couponIssue1 = CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .issuedAt(LocalDateTime.now().minusDays(1))
                .build();
        ReflectionTestUtils.setField(couponIssue1, "id", 1L);
        
        Coupon coupon2 = Coupon.builder()
                .title("두 번째 쿠폰")
                .totalQuantity(50)
                .validFrom(LocalDateTime.now().minusDays(2))
                .validTo(LocalDateTime.now().plusDays(20))
                .status(CouponStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(coupon2, "id", 2L);
        
        CouponIssue couponIssue2 = CouponIssue.builder()
                .user(user)
                .coupon(coupon2)
                .issuedAt(LocalDateTime.now().minusDays(2))
                .build();
        ReflectionTestUtils.setField(couponIssue2, "id", 2L);
        
        List<CouponIssue> couponIssues = Arrays.asList(couponIssue1, couponIssue2);
        
        given(userService.findById(1L)).willReturn(user);
        given(couponIssueRepository.findByUserIdWithCoupon(1L)).willReturn(couponIssues);
        
        // when
        List<CouponIssueResponse> responses = couponService.getIssuedCoupons(1L);
        
        // then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getCouponId()).isEqualTo(1L);
        assertThat(responses.get(0).getCouponTitle()).isEqualTo("테스트 쿠폰");
        assertThat(responses.get(1).getCouponId()).isEqualTo(2L);
        assertThat(responses.get(1).getCouponTitle()).isEqualTo("두 번째 쿠폰");
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자의 발급 쿠폰 조회 시 예외 발생")
    void getIssuedCoupons_UserNotFound() {
        // given
        given(userService.findById(999L)).willThrow(new IllegalArgumentException("존재하지 않는 사용자입니다. id=999"));
        
        // when & then
        assertThatThrownBy(() -> couponService.getIssuedCoupons(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다. id=999");
    }
}
