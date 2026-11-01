package com.pos.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.user.service.AuthService;
import com.pos.user.dto.UserDtos.LoginRequest;
import com.pos.user.dto.UserDtos.LoginResponse;
import com.pos.user.dto.UserDtos.PinLoginRequest;
import com.pos.user.dto.UserDtos.UserView;

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

    /** Ends the session: the presented token is revoked immediately. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout();
    }

    @GetMapping("/me")
    public UserView me() {
        return authService.me();
    }
}
