package com.coupon.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("사용자 생성 테스트")
    void createUser() {
        // given
        String email = "test@example.com";
        String name = "테스트사용자";

        // when
        User user = User.builder()
                .email(email)
                .name(name)
                .build();

        // then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 생성 시 이메일은 필수값이다")
    void createUserWithoutEmail() {
        // given
        String name = "테스트사용자";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> 
            User.builder()
                .name(name)
                .build()
        );
    }

    @Test
    @DisplayName("사용자 생성 시 이름은 필수값이다")
    void createUserWithoutName() {
        // given
        String email = "test@example.com";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> 
            User.builder()
                .email(email)
                .build()
        );
    }
}
