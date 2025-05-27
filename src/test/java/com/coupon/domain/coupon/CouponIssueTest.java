package com.coupon.domain.coupon;

import com.coupon.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CouponIssueTest {

    private User user;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("테스트사용자")
                .build();

        coupon = Coupon.builder()
                .title("테스트 쿠폰")
                .totalQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("쿠폰 발급 생성 테스트")
    void createCouponIssue() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        CouponIssue couponIssue = CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .issuedAt(now)
                .build();

        // then
        assertThat(couponIssue).isNotNull();
        assertThat(couponIssue.getUser()).isEqualTo(user);
        assertThat(couponIssue.getCoupon()).isEqualTo(coupon);
        assertThat(couponIssue.getIssuedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("쿠폰 발급 생성 시 발급일이 없으면 현재 시간으로 설정된다")
    void createCouponIssue_WithoutIssuedAt() {
        // when
        CouponIssue couponIssue = CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .build();

        // then
        assertThat(couponIssue).isNotNull();
        assertThat(couponIssue.getIssuedAt()).isNotNull();
    }
}
