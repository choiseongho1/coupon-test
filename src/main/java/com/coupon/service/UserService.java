package com.coupon.service;

import com.coupon.domain.user.User;
import com.coupon.dto.user.UserRegisterRequest;
import com.coupon.dto.user.UserResponse;
import com.coupon.exception.DuplicateEmailException;
import com.coupon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse registerUser(UserRegisterRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .build();

        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser);
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));
    }
}
