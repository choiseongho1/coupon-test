package com.coupon.repository;

import com.coupon.domain.coupon.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long>, CouponIssueRepositoryCustom {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
    
    /**
     * 사용자가 발급받은 쿠폰 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 발급받은 쿠폰 목록
     */
    @Query("SELECT ci FROM CouponIssue ci JOIN FETCH ci.coupon WHERE ci.user.id = :userId ORDER BY ci.issuedAt DESC")
    List<CouponIssue> findByUserIdWithCoupon(@Param("userId") Long userId);
    
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
    
    /**
     * 특정 쿠폰의 오늘 발급 수량을 조회합니다.
     *
     * @param couponId 쿠폰 ID
     * @param today 오늘 날짜
     * @return 발급 수량
     */
    @Query("SELECT COUNT(ci) FROM CouponIssue ci " +
           "WHERE ci.coupon.id = :couponId " +
           "AND FUNCTION('DATE', ci.issuedAt) = :today")
    long countByCouponIdIssuedToday(
            @Param("couponId") Long couponId,
            @Param("today") LocalDate today
    );
    
    /**
     * 특정 쿠폰의 이번 주 발급 수량을 조회합니다.
     *
     * @param couponId 쿠폰 ID
     * @param startOfWeek 주의 시작일
     * @param endOfWeek 주의 마지막일
     * @return 발급 수량
     */
    @Query("SELECT COUNT(ci) FROM CouponIssue ci " +
           "WHERE ci.coupon.id = :couponId " +
           "AND ci.issuedAt BETWEEN :startOfWeek AND :endOfWeek")
    long countByCouponIdIssuedThisWeek(
            @Param("couponId") Long couponId,
            @Param("startOfWeek") LocalDateTime startOfWeek,
            @Param("endOfWeek") LocalDateTime endOfWeek
    );
    
    /**
     * 특정 쿠폰의 이번 달 발급 수량을 조회합니다.
     *
     * @param couponId 쿠폰 ID
     * @param startOfMonth 달의 시작일
     * @param endOfMonth 달의 마지막일
     * @return 발급 수량
     */
    @Query("SELECT COUNT(ci) FROM CouponIssue ci " +
           "WHERE ci.coupon.id = :couponId " +
           "AND ci.issuedAt BETWEEN :startOfMonth AND :endOfMonth")
    long countByCouponIdIssuedThisMonth(
            @Param("couponId") Long couponId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );
    
    /**
     * 전체 쿠폰의 오늘 발급 수량을 조회합니다.
     *
     * @param today 오늘 날짜
     * @return 발급 수량
     */
    @Query("SELECT COUNT(ci) FROM CouponIssue ci " +
           "WHERE FUNCTION('DATE', ci.issuedAt) = :today")
    long countAllIssuedToday(@Param("today") LocalDate today);
    
    /**
     * 전체 쿠폰의 이번 주 발급 수량을 조회합니다.
     *
     * @param startOfWeek 주의 시작일
     * @param endOfWeek 주의 마지막일
     * @return 발급 수량
     */
    @Query("SELECT COUNT(ci) FROM CouponIssue ci " +
           "WHERE ci.issuedAt BETWEEN :startOfWeek AND :endOfWeek")
    long countAllIssuedThisWeek(
            @Param("startOfWeek") LocalDateTime startOfWeek,
            @Param("endOfWeek") LocalDateTime endOfWeek
    );
    
    /**
     * 전체 쿠폰의 이번 달 발급 수량을 조회합니다.
     *
     * @param startOfMonth 달의 시작일
     * @param endOfMonth 달의 마지막일
     * @return 발급 수량
     */
    @Query("SELECT COUNT(ci) FROM CouponIssue ci " +
           "WHERE ci.issuedAt BETWEEN :startOfMonth AND :endOfMonth")
    long countAllIssuedThisMonth(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );
}
