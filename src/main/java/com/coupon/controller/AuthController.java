package com.coupon.controller;

import com.coupon.dto.auth.LoginRequest;
import com.coupon.dto.auth.TokenResponse;
import com.coupon.dto.user.UserRegisterRequest;
import com.coupon.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.login(loginRequest);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody UserRegisterRequest signUpRequest) {
        TokenResponse tokenResponse = authService.register(
            signUpRequest.getEmail(),
            signUpRequest.getPassword(),
            signUpRequest.getName()
        );
        return ResponseEntity.ok(tokenResponse);
    }
}
