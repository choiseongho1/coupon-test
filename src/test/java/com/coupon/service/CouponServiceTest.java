package com.coupon.service;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponIssue;
import com.coupon.domain.user.User;
import com.coupon.dto.coupon.CouponCreateRequest;
import com.coupon.dto.coupon.CouponIssueResponse;
import com.coupon.dto.coupon.CouponResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserService userService;

    @Mock
    private CouponIssueRepository couponIssueRepository;

    @InjectMocks
    private CouponService couponService;

    private User user;
    private Coupon coupon;
    private CouponCreateRequest createRequest;

    @BeforeEach
    void setUp() {
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
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {
        // given
        CouponIssue couponIssue = CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .issuedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(couponIssue, "id", 1L);

        given(userService.findById(1L)).willReturn(user);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(false);
        given(couponIssueRepository.existsByUserIdAndCouponIdToday(1L, 1L)).willReturn(false);
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
    @DisplayName("존재하지 않는 사용자로 쿠폰 발급 시도 시 예외 발생")
    void issueCoupon_UserNotFound() {
        // given
        given(userService.findById(999L)).willThrow(new IllegalArgumentException("존재하지 않는 사용자입니다. id=999"));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다. id=999");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 발급 시도 시 예외 발생")
    void issueCoupon_CouponNotFound() {
        // given
        given(userService.findById(1L)).willReturn(user);
        given(couponRepository.findByIdWithPessimisticLock(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 쿠폰입니다. id=999");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰 재발급 시도 시 예외 발생")
    void issueCoupon_AlreadyIssued() {
        // given
        given(userService.findById(1L)).willReturn(user);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.existsByUserIdAndCouponId(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponAlreadyIssuedException.class)
                .hasMessage("이미 발급받은 쿠폰입니다.");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 수량이 소진된 경우 예외 발생")
    void issueCoupon_CouponExhausted() {
        // given
        ReflectionTestUtils.setField(coupon, "remainingQuantity", 0);
        
        given(userService.findById(1L)).willReturn(user);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponExhaustedException.class)
                .hasMessage("쿠폰이 모두 소진되었습니다.");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("쿠폰 유효기간이 지난 경우 예외 발생")
    void issueCoupon_CouponExpired() {
        // given
        coupon = Coupon.builder()
                .title("만료된 쿠폰")
                .totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(30))
                .validTo(LocalDateTime.now().minusDays(1))
                .build();
        ReflectionTestUtils.setField(coupon, "id", 1L);
        ReflectionTestUtils.setField(coupon, "remainingQuantity", 100);
        
        given(userService.findById(1L)).willReturn(user);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(CouponExpiredException.class)
                .hasMessage("만료된 쿠폰입니다.");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("일일 발급 한도 초과 시 예외 발생")
    void issueCoupon_DailyLimitExceeded() {
        // given
        given(userService.findById(1L)).willReturn(user);
        given(couponRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(coupon));
        given(couponIssueRepository.existsByUserIdAndCouponIdToday(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessage("하루에 한 번만 발급 가능한 쿠폰입니다.");
                
        verify(couponRepository, never()).save(any());
        verify(couponIssueRepository, never()).save(any());
    }
}
