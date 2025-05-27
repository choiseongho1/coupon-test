package com.coupon.repository;

import com.coupon.domain.coupon.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long>, CouponIssueRepositoryCustom {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
    
    /**
     * 특정 기간 동안 발급된 쿠폰의 수를 조회합니다.
     *
     * @param couponId 쿠폰 ID
     * @param startDate 조회 시작 일시
     * @param endDate 조회 종료 일시
     * @return 발급된 쿠폰 수
     */
    @Query("SELECT COUNT(ci) FROM CouponIssue ci " +
           "WHERE ci.coupon.id = :couponId " +
           "AND ci.issuedAt BETWEEN :startDate AND :endDate")
    long countByCouponIdAndIssuedAtBetween(
            @Param("couponId") Long couponId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
