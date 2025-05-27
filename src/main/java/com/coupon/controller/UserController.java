package com.coupon.controller;

import com.coupon.domain.user.User;
import com.coupon.dto.ApiResponse;
import com.coupon.dto.user.UserResponse;
import com.coupon.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(new UserResponse(user), "회원 정보 조회 성공"));
    }
}
