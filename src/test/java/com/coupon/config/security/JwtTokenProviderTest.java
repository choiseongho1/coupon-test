package com.coupon.config.security;

import com.coupon.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private CustomUserDetailsService userDetailsService;
    
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_SECRET_KEY = "test-secret-key-12345678901234567890123456789012";

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 설정
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenProvider, "tokenValidityInMilliseconds", 3600000L);
        
        // 시크릿 키 초기화
        byte[] keyBytes = Base64.getEncoder().encode(TEST_SECRET_KEY.getBytes());
        Key key = Keys.hmacShaKeyFor(keyBytes);
        ReflectionTestUtils.setField(jwtTokenProvider, "key", key);
    }

    @Test
    @DisplayName("JWT 토큰 생성 테스트")
    void createToken() {
        // when
        String token = jwtTokenProvider.createToken(TEST_EMAIL);
        
        // then
        assertNotNull(token);
        
        // 토큰 검증
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(ReflectionTestUtils.getField(jwtTokenProvider, "key"))
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        assertEquals(TEST_EMAIL, claims.getSubject());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    @DisplayName("잘못된 토큰 검증 테스트")
    void validateInvalidToken() {
        // given
        String invalidToken = "invalid.token.here";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("JWT 토큰에서 사용자 이메일 추출 테스트")
    void getUserEmailFromToken() {
        // given
        String token = jwtTokenProvider.createToken(TEST_EMAIL);
        
        // when
        String userEmail = jwtTokenProvider.getUserEmail(token);
        
        // then
        assertEquals(TEST_EMAIL, userEmail);
    }
    
    @Test
    @DisplayName("JWT 토큰에서 인증 객체 추출 테스트")
    void getAuthenticationFromToken() {
        // given
        UserDetails userDetails = new User(TEST_EMAIL, "", new ArrayList<>());
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
        String token = jwtTokenProvider.createToken(TEST_EMAIL);
        
        // when & then
        assertDoesNotThrow(() -> jwtTokenProvider.getAuthentication(token));
    }
}
