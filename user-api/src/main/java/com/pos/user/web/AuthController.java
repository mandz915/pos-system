package com.pos.user.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pos.user.service.AuthService;
import com.pos.user.web.UserDtos.LoginRequest;
import com.pos.user.web.UserDtos.LoginResponse;
import com.pos.user.web.UserDtos.PinLoginRequest;
import com.pos.user.web.UserDtos.UserView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Username + password login (back office, managers). */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Fast cashier login at the till: username + numeric PIN. */
    @PostMapping("/pin-login")
    public LoginResponse pinLogin(@Valid @RequestBody PinLoginRequest request) {
        return authService.pinLogin(request);
    }

    @GetMapping("/me")
    public UserView me() {
        return authService.me();
    }
}
