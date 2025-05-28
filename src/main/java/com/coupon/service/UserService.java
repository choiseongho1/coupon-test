package com.coupon.service;

import com.coupon.domain.user.User;
import com.coupon.domain.user.UserRole;
import com.coupon.dto.user.UserRegisterRequest;
import com.coupon.dto.user.UserResponse;
import com.coupon.exception.DuplicateEmailException;
import com.coupon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 등록 요청을 처리합니다.
     * 
     * @param request 사용자 등록 요청 정보 (이메일, 비밀번호, 이름)
     * @return 등록된 사용자 정보
     * @throws DuplicateEmailException 이미 존재하는 이메일인 경우
     */
    @Transactional
    public UserResponse registerUser(UserRegisterRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser);
    }
    
    /**
     * 사용자 등록을 처리합니다.
     * 
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @param name 사용자 이름
     * @return 등록된 사용자 엔티티
     * @throws DuplicateEmailException 이미 존재하는 이메일인 경우
     */
    @Transactional
    public User registerUser(String email, String password, String name) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(password))
                .role(UserRole.USER)
                .build();

        return userRepository.save(user);
    }

    /**
     * 사용자 ID로 사용자를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자 엔티티
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));
    }
}
