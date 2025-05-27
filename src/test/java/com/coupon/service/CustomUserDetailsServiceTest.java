package com.coupon.service;

import com.coupon.domain.user.User;
import com.coupon.domain.user.UserRole;
import com.coupon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NAME = "테스트사용자";

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .password(TEST_PASSWORD)
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void loadUserByUsername_Success() {
        // given
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_EMAIL);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(TEST_EMAIL);
        assertThat(userDetails.getPassword()).isEqualTo(TEST_PASSWORD);
        assertThat(userDetails.getAuthorities()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 사용자 조회 시 예외 발생")
    void loadUserByUsername_UserNotFound() {
        // given
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: " + nonExistentEmail);
    }
}
