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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status = CouponStatus.ACTIVE;

    @Builder
    public Coupon(String title, Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validTo, CouponStatus status) {
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
        this.status = status != null ? status : CouponStatus.ACTIVE;
    }

    @Builder
    public Coupon(String title, Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validTo, CouponStatus status, Long id) {
        
        this.title = title;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status != null ? status : CouponStatus.ACTIVE;
        this.id = id;
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
        return status == CouponStatus.ACTIVE && 
               remainingQuantity > 0 && 
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
    
    /**
     * 테스트 목적으로 쿠폰의 남은 수량을 직접 설정합니다.
     * 실제 운영 환경에서는 사용하지 않아야 합니다.
     * 
     * @param quantity 설정할 남은 수량
     */
    public void updateRemainingQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Remaining quantity must be greater than or equal to 0");
        }
        this.remainingQuantity = quantity;
        
        // 재고가 0이 되면 상태를 EXHAUSTED로 변경
        if (quantity == 0 && this.status == CouponStatus.ACTIVE) {
            this.status = CouponStatus.EXHAUSTED;
        }
    }
    
    /**
     * 쿠폰의 상태를 변경합니다.
     * 
     * @param status 변경할 상태
     */
    public void setStatus(CouponStatus status) {
        this.status = status;
    }
}
