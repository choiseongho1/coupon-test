package com.coupon.repository;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.coupon.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdWithPessimisticLock(@Param("id") Long id);
    
    /**
     * 만료일이 지났지만 아직 잔여 수량이 남아 있는 쿠폰 조회
     */
    List<Coupon> findByValidToBeforeAndRemainingQuantityGreaterThan(
            LocalDateTime validTo, int remainingQuantity);
            
    /**
     * 제목으로 쿠폰 조회
     */
    Optional<Coupon> findByTitle(String title);
    
    /**
     * 특정 상태의 쿠폰 수를 조회합니다.
     * 
     * @param status 쿠폰 상태
     * @return 해당 상태의 쿠폰 수
     */
    long countByStatus(CouponStatus status);
    
    /**
     * 전체 쿠폰의 총 수량과 남은 수량을 조회합니다.
     * 
     * @return 총 수량과 남은 수량을 담은 Object[] 배열 (totalQuantity, remainingQuantity)
     */
    @Query("SELECT SUM(c.totalQuantity), SUM(c.remainingQuantity) FROM Coupon c")
    Object[] getTotalAndRemainingQuantity();
}
