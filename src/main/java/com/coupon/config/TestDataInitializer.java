package com.coupon.config;

import com.coupon.domain.coupon.Coupon;
import com.coupon.domain.user.User;
import com.coupon.repository.CouponRepository;
import com.coupon.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test") // 테스트 환경에서는 실행하지 않음
public class TestDataInitializer {

    // private final UserRepository userRepository;
    // private final CouponRepository couponRepository;

    // @PostConstruct
    // @Transactional
    // public void init() {
    //     log.info("Initializing test data...");
        
    //     // 테스트 사용자 생성
    //     User user1 = createUser("user1@example.com", "사용자1");
    //     User user2 = createUser("user2@example.com", "사용자2");
        
    //     // 테스트 쿠폰 생성
    //     createCoupon("신규 가입 축하 쿠폰", 100, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
    //     createCoupon("여름 할인 쿠폰", 50, LocalDateTime.now(), LocalDateTime.now().plusDays(7));
    //     createCoupon("한정 수량 쿠폰", 10, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        
    //     log.info("Test data initialization completed.");
    // }
    
    // private User createUser(String email, String name) {
    //     if (!userRepository.existsByEmail(email)) {
    //         User user = User.builder()
    //                 .email(email)
    //                 .name(name)
    //                 .password("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi") // password is 'password'
    //                 .role(UserRole.USER)
    //                 .build();
    //         return userRepository.save(user);
    //     }
    //     return userRepository.findByEmail(email).orElseThrow();
    // }
    
    // private void createCoupon(String title, int totalQuantity, LocalDateTime validFrom, LocalDateTime validTo) {
    //     if (couponRepository.findByTitle(title).isEmpty()) {
    //         Coupon coupon = Coupon.builder()
    //                 .title(title)
    //                 .totalQuantity(totalQuantity)
    //                 .validFrom(validFrom)
    //                 .validTo(validTo)
    //                 .build();
    //         couponRepository.save(coupon);
    //     }
    // }
}
