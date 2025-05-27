package com.coupon.service;

import com.coupon.domain.user.User;
import com.coupon.dto.user.UserRegisterRequest;
import com.coupon.dto.user.UserResponse;
import com.coupon.exception.DuplicateEmailException;
import com.coupon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new UserRegisterRequest("test@example.com", "테스트사용자", "password123!");
        user = User.builder()
                .email(registerRequest.getEmail())
                .name(registerRequest.getName())
                .password("encodedPassword") // Mock the encoded password
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("사용자 등록 성공")
    void registerUser_Success() {
        // given
        given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(passwordEncoder.encode(registerRequest.getPassword())).willReturn("encodedPassword");

        // when
        UserResponse response = userService.registerUser(registerRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(response.getName()).isEqualTo(registerRequest.getName());
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(registerRequest.getPassword());
    }

    @Test
    @DisplayName("중복 이메일로 사용자 등록 시 예외 발생")
    void registerUser_DuplicateEmail() {
        // given
        given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(registerRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");
                
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void getUserById_Success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        User foundUser = userService.findById(1L);

        // then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(1L);
        assertThat(foundUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(foundUser.getName()).isEqualTo(user.getName());
        
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 예외 발생")
    void getUserById_NotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다. id=999");
                
        verify(userRepository).findById(999L);
    }
}
