package com.coupon.service;

import com.coupon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security의 인증을 위한 UserDetailsService 구현체입니다.
 * 사용자 이메일을 기반으로 사용자 정보를 조회합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 이메일을 기반으로 사용자 정보를 조회합니다.
     * 
     * @param email 사용자 이메일
     * @return Spring Security에서 사용할 UserDetails 객체
     * @throws UsernameNotFoundException 사용자가 존재하지 않는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
