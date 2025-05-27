package com.coupon.service;

import com.coupon.config.security.JwtTokenProvider;
import com.coupon.domain.user.User;
import com.coupon.domain.user.UserRole;
import com.coupon.dto.auth.LoginRequest;
import com.coupon.dto.auth.TokenResponse;
import com.coupon.exception.DuplicateEmailException;
import com.coupon.exception.LoginFailException;
import com.coupon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NAME = "테스트사용자";
    private static final String TEST_TOKEN = "test.jwt.token";

    private LoginRequest loginRequest;
    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        
        user = User.builder()
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();
                
        authentication = new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, user.getAuthorities());
        
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccess() {
        // given
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        // when
        TokenResponse response = authService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_TOKEN);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 자격 증명")
    void loginFailWithBadCredentials() {
        // given
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LoginFailException.class)
                .hasMessageContaining("로그인에 실패했습니다");
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void registerSuccess() {
        // given
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        // when
        TokenResponse response = authService.register(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_TOKEN);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 중복")
    void registerFailWithDuplicateEmail() {
        // given
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.register(TEST_EMAIL, TEST_PASSWORD, TEST_NAME))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다");
    }
}
