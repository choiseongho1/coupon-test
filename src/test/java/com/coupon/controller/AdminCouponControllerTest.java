package com.coupon.controller;

import com.coupon.domain.coupon.CouponStatus;
import com.coupon.dto.coupon.CouponStatisticsResponse;
import com.coupon.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCouponControllerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private AdminCouponController adminCouponController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminCouponController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("특정 쿠폰의 통계 조회 성공")
    void getCouponStatistics_Success() throws Exception {
        // given
        CouponStatisticsResponse statisticsResponse = CouponStatisticsResponse.builder()
                .couponId(1L)
                .couponTitle("테스트 쿠폰")
                .status(CouponStatus.ACTIVE)
                .totalQuantity(100)
                .remainingQuantity(50)
                .issuedQuantity(50)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .issuedToday(10L)
                .issuedThisWeek(30L)
                .issuedThisMonth(50L)
                .build();

        given(couponService.getCouponStatistics(anyLong())).willReturn(statisticsResponse);

        // when & then
        mockMvc.perform(get("/api/admin/coupons/1/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponId").value(1))
                .andExpect(jsonPath("$.data.couponTitle").value("테스트 쿠폰"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalQuantity").value(100))
                .andExpect(jsonPath("$.data.remainingQuantity").value(50))
                .andExpect(jsonPath("$.data.issuedQuantity").value(50))
                .andExpect(jsonPath("$.data.issuedToday").value(10))
                .andExpect(jsonPath("$.data.issuedThisWeek").value(30))
                .andExpect(jsonPath("$.data.issuedThisMonth").value(50));
    }

    @Test
    @DisplayName("전체 쿠폰 통계 조회 성공")
    void getAllCouponsStatistics_Success() throws Exception {
        // given
        CouponStatisticsResponse statisticsResponse = CouponStatisticsResponse.builder()
                .couponTitle("전체 쿠폰 통계")
                .totalQuantity(500)
                .remainingQuantity(300)
                .issuedQuantity(200)
                .issuedToday(20L)
                .issuedThisWeek(80L)
                .issuedThisMonth(200L)
                .build();

        given(couponService.getAllCouponsStatistics()).willReturn(statisticsResponse);

        // when & then
        mockMvc.perform(get("/api/admin/coupons/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponTitle").value("전체 쿠폰 통계"))
                .andExpect(jsonPath("$.data.totalQuantity").value(500))
                .andExpect(jsonPath("$.data.remainingQuantity").value(300))
                .andExpect(jsonPath("$.data.issuedQuantity").value(200))
                .andExpect(jsonPath("$.data.issuedToday").value(20))
                .andExpect(jsonPath("$.data.issuedThisWeek").value(80))
                .andExpect(jsonPath("$.data.issuedThisMonth").value(200));
    }
}
