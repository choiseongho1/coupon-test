package com.coupon.domain.coupon;

import com.coupon.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_issue",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "user_coupon_unique",
               columnNames = {"user_id", "coupon_id"}
           )
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Builder
    public CouponIssue(User user, Coupon coupon, LocalDateTime issuedAt) {
        this.user = user;
        this.coupon = coupon;
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
    }

    public static CouponIssue issueCoupon(User user, Coupon coupon) {
        coupon.decreaseRemainingQuantity();
        return CouponIssue.builder()
                .user(user)
                .coupon(coupon)
                .issuedAt(LocalDateTime.now())
                .build();
    }
}
