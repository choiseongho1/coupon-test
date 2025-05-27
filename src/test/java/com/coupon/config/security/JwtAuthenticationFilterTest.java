package com.coupon.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;
    
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;
    
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 요청 시 인증 성공")
    void validTokenShouldAuthenticateUser() throws ServletException, IOException {
        // given
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        
        Authentication mockAuth = mock(Authentication.class);
        when(tokenProvider.getAuthentication(token)).thenReturn(mockAuth);
        
        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider).validateToken(token);
        verify(tokenProvider).getAuthentication(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 요청 시 인증 실패")
    void invalidTokenShouldNotAuthenticate() throws ServletException, IOException {
        // given
        String invalidToken = "invalid.token";
        request.addHeader("Authorization", "Bearer " + invalidToken);
        when(tokenProvider.validateToken(invalidToken)).thenReturn(false);
        
        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider).validateToken(invalidToken);
        verify(tokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 없이 요청 시 인증 실패")
    void noTokenShouldNotAuthenticate() throws ServletException, IOException {
        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(tokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    @DisplayName("Authorization 헤더가 Bearer로 시작하지 않을 때")
    void nonBearerTokenShouldNotAuthenticate() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Basic " + "sometoken");
        
        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(tokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
