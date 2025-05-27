package com.coupon.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public class CouponIssueRepositoryImpl implements CouponIssueRepositoryCustom {
    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean existsByUserIdAndCouponIdToday(Long userId, Long couponId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        Long count = em.createQuery(
                "SELECT COUNT(ci) FROM CouponIssue ci " +
                "WHERE ci.user.id = :userId " +
                "AND ci.coupon.id = :couponId " +
                "AND ci.issuedAt >= :startOfDay " +
                "AND ci.issuedAt < :endOfDay", Long.class)
                .setParameter("userId", userId)
                .setParameter("couponId", couponId)
                .setParameter("startOfDay", startOfDay)
                .setParameter("endOfDay", endOfDay)
                .getSingleResult();
        
        return count > 0;
    }
}
