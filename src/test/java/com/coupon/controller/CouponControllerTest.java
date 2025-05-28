package com.coupon.controller;

import com.coupon.dto.coupon.CouponCreateRequest;
import com.coupon.dto.coupon.CouponIssueResponse;
import com.coupon.dto.coupon.CouponResponse;
import com.coupon.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(couponController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 직렬화를 위해 필요
    }

    @Test
    @DisplayName("쿠폰 생성 성공")
    void createCoupon_Success() throws Exception {
        // given
        CouponCreateRequest request = new CouponCreateRequest(
                "테스트 쿠폰",
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        // Mockito를 사용하여 CouponResponse 객체 생성
        CouponResponse response = Mockito.mock(CouponResponse.class);
        Mockito.when(response.getId()).thenReturn(1L);
        Mockito.when(response.getTitle()).thenReturn("테스트 쿠폰");
        Mockito.when(response.getTotalQuantity()).thenReturn(100);
        Mockito.when(response.getRemainingQuantity()).thenReturn(100);

        given(couponService.createCoupon(any(CouponCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("테스트 쿠폰"))
                .andExpect(jsonPath("$.data.totalQuantity").value(100))
                .andExpect(jsonPath("$.data.remainingQuantity").value(100));
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() throws Exception {
        // given
        CouponIssueResponse issueResponse = new CouponIssueResponse(
                1L, 1L, 1L, LocalDateTime.now(), "테스트 쿠폰", 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        
        given(couponService.issueCoupon(anyLong(), anyLong())).willReturn(issueResponse);

        // when & then
        mockMvc.perform(post("/api/coupons/1/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("쿠폰이 발급되었습니다."));
    }

    @Test
    @DisplayName("모든 쿠폰 조회 성공")
    void getAllCoupons_Success() throws Exception {
        // given
        // Mockito를 사용하여 CouponResponse 객체 생성
        CouponResponse coupon1 = Mockito.mock(CouponResponse.class);
        Mockito.when(coupon1.getId()).thenReturn(1L);
        Mockito.when(coupon1.getTitle()).thenReturn("테스트 쿠폰 1");
        Mockito.when(coupon1.getTotalQuantity()).thenReturn(100);
        Mockito.when(coupon1.getRemainingQuantity()).thenReturn(50);

        CouponResponse coupon2 = Mockito.mock(CouponResponse.class);
        Mockito.when(coupon2.getId()).thenReturn(2L);
        Mockito.when(coupon2.getTitle()).thenReturn("테스트 쿠폰 2");
        Mockito.when(coupon2.getTotalQuantity()).thenReturn(200);
        Mockito.when(coupon2.getRemainingQuantity()).thenReturn(150);

        List<CouponResponse> coupons = Arrays.asList(coupon1, coupon2);

        given(couponService.getAllCoupons()).willReturn(coupons);

        // when & then
        mockMvc.perform(get("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("테스트 쿠폰 1"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].title").value("테스트 쿠폰 2"));
    }

    @Test
    @DisplayName("내 발급 쿠폰 조회 성공")
    void getMyIssuedCoupons_Success() throws Exception {
        // given
        CouponIssueResponse couponIssue1 = new CouponIssueResponse(
                1L, 1L, 1L, LocalDateTime.now().minusDays(1),
                "테스트 쿠폰 1", LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(25)
        );

        CouponIssueResponse couponIssue2 = new CouponIssueResponse(
                2L, 1L, 2L, LocalDateTime.now().minusDays(2),
                "테스트 쿠폰 2", LocalDateTime.now().minusDays(10), LocalDateTime.now().plusDays(20)
        );

        List<CouponIssueResponse> issuedCoupons = Arrays.asList(couponIssue1, couponIssue2);

        given(couponService.getIssuedCoupons(anyLong())).willReturn(issuedCoupons);

        // when & then
        mockMvc.perform(get("/api/coupons/my")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].couponId").value(1))
                .andExpect(jsonPath("$.data[0].couponTitle").value("테스트 쿠폰 1"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].couponId").value(2))
                .andExpect(jsonPath("$.data[1].couponTitle").value("테스트 쿠폰 2"));
    }
}
