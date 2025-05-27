package com.coupon.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    @Test
    @DisplayName("쿠폰 생성 테스트")
    void createCoupon() {
        // given
        String title = "신규 가입 축하 쿠폰";
        int totalQuantity = 100;
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = validFrom.plusDays(30);

        // when
        Coupon coupon = Coupon.builder()
                .title(title)
                .totalQuantity(totalQuantity)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();

        // then
        assertThat(coupon).isNotNull();
        assertThat(coupon.getTitle()).isEqualTo(title);
        assertThat(coupon.getTotalQuantity()).isEqualTo(totalQuantity);
        assertThat(coupon.getRemainingQuantity()).isEqualTo(totalQuantity);
        assertThat(coupon.getValidFrom()).isEqualTo(validFrom);
        assertThat(coupon.getValidTo()).isEqualTo(validTo);
        assertThat(coupon.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 유효한 경우")
    void isIssuable_Valid() {
        // given
        Coupon coupon = createTestCoupon(10, 5, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        // when
        boolean result = coupon.isIssuable();

        // then
        assertThat(result).isTrue();
    }


    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 수량이 없는 경우")
    void isIssuable_NoRemainingQuantity() {
        // given
        Coupon coupon = createTestCoupon(10, 0, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        // when
        boolean result = coupon.isIssuable();
        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 유효기간이 지난 경우")
    void isIssuable_Expired() {
        // given
        Coupon coupon = createTestCoupon(10, 5, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));

        // when
        boolean result = coupon.isIssuable();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 가능 여부 확인 - 아직 유효기간이 시작되지 않은 경우")
    void isIssuable_NotStarted() {
        // given
        Coupon coupon = createTestCoupon(10, 5, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        // when
        boolean result = coupon.isIssuable();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 - 성공")
    void issue_Success() {
        // given
        Coupon coupon = createTestCoupon(10, 5, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        int beforeRemaining = coupon.getRemainingQuantity();

        // when
        coupon.issue();


        // then
        assertThat(coupon.getRemainingQuantity()).isEqualTo(beforeRemaining - 1);
    }

    @Test
    @DisplayName("쿠폰 발급 - 수량이 없는 경우 예외 발생")
    void issue_NoRemainingQuantity() {
        // given
        Coupon coupon = createTestCoupon(10, 0, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        // when & then
        assertThatThrownBy(coupon::issue)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("발급 가능한 쿠폰이 없습니다.");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("유효하지 않은 수량으로 쿠폰 생성 시 예외 발생")
    void createCoupon_InvalidQuantity(int quantity) {
        // given
        String title = "유효하지 않은 수량 쿠폰";
        LocalDateTime now = LocalDateTime.now();

        // when & then
        assertThatThrownBy(() -> Coupon.builder()
                .title(title)
                .totalQuantity(quantity)
                .validFrom(now)
                .validTo(now.plusDays(30))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total quantity must be greater than 0");
    }

    @Test
    @DisplayName("유효기간이 잘못된 경우 예외 발생")
    void createCoupon_InvalidValidPeriod() {
        // given
        String title = "유효기간 잘못된 쿠폰";
        LocalDateTime now = LocalDateTime.now();

        // when & then
        assertThatThrownBy(() -> Coupon.builder()
                .title(title)
                .totalQuantity(100)
                .validFrom(now.plusDays(1))  // 시작일이 종료일보다 나중
                .validTo(now)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valid from must be before valid to");
    }

    private Coupon createTestCoupon(int totalQuantity, int remainingQuantity, LocalDateTime validFrom, LocalDateTime validTo) {
        Coupon coupon = Coupon.builder()
                .title("테스트 쿠폰")
                .totalQuantity(totalQuantity)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
        
        // 리플렉션을 사용하여 remainingQuantity 설정 (테스트용)
        try {
            var field = Coupon.class.getDeclaredField("remainingQuantity");
            field.setAccessible(true);
            field.set(coupon, remainingQuantity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return coupon;
    }
}
