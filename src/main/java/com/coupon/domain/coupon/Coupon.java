package com.coupon.domain.coupon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public Coupon(String title, Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validTo) {
        if (totalQuantity == null || totalQuantity <= 0) {
            throw new IllegalArgumentException("Total quantity must be greater than 0");
        }
        if (validFrom == null || validTo == null) {
            throw new IllegalArgumentException("Valid from and valid to dates must be provided");
        }
        if (!validFrom.isBefore(validTo)) {
            throw new IllegalArgumentException("Valid from must be before valid to");
        }
        
        this.title = title;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public void decreaseRemainingQuantity() {
        if (this.remainingQuantity <= 0) {
            throw new IllegalStateException("No remaining coupons available");
        }
        this.remainingQuantity--;
    }
    
    /**
     * 쿠폰 발급 가능 여부를 확인합니다.
     * @return 발급 가능하면 true, 그렇지 않으면 false
     */
    public boolean isIssuable() {
        LocalDateTime now = LocalDateTime.now();
        return remainingQuantity > 0 && 
               !now.isBefore(validFrom) && 
               !now.isAfter(validTo);
    }
    
    /**
     * 쿠폰을 발급합니다.
     * @throws IllegalStateException 발급 가능한 쿠폰이 없는 경우
     */
    public void issue() {
        if (!isIssuable()) {
            throw new IllegalStateException("발급 가능한 쿠폰이 없습니다.");
        }
        this.remainingQuantity--;
    }
}
