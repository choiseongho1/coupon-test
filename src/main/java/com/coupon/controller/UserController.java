package com.coupon.controller;

import com.coupon.dto.ApiResponse;
import com.coupon.dto.user.UserRegisterRequest;
import com.coupon.dto.user.UserResponse;
import com.coupon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        return ApiResponse.success(response, "회원가입이 완료되었습니다.");
    }
}
