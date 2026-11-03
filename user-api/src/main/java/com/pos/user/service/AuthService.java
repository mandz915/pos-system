package com.pos.user.service;

import com.pos.user.dto.User;
import com.pos.user.dto.UserDtos.LoginRequest;
import com.pos.user.dto.UserDtos.LoginResponse;
import com.pos.user.dto.UserDtos.PinLoginRequest;
import com.pos.user.dto.UserDtos.UserView;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse pinLogin(PinLoginRequest request);

    void logout();

    UserView me();

    UserView toView(User user);
}
