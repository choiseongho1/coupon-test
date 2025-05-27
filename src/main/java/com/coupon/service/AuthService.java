package com.coupon.service;

import com.coupon.config.security.JwtTokenProvider;
import com.coupon.domain.user.User;
import com.coupon.domain.user.UserRole;
import com.coupon.dto.auth.LoginRequest;
import com.coupon.dto.auth.TokenResponse;
import com.coupon.exception.DuplicateEmailException;
import com.coupon.exception.LoginFailException;
import com.coupon.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.createToken(authentication.getName());
            return new TokenResponse(jwt, "Bearer");
        } catch (Exception e) {
            throw new LoginFailException("로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.");
        }
    }

    @Transactional
    public TokenResponse register(String email, String password, String name) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);
        
        // 가입 후 자동 로그인
        String jwt = tokenProvider.createToken(savedUser.getEmail());
        return new TokenResponse(jwt, "Bearer");
    }
}
