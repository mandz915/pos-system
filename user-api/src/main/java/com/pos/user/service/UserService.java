package com.pos.user.service;

import java.util.List;

import com.pos.common.dto.PageResult;
import com.pos.user.dto.UserDtos.ChangePasswordRequest;
import com.pos.user.dto.UserDtos.CreateUserRequest;
import com.pos.user.dto.UserDtos.UpdateUserRequest;
import com.pos.user.dto.UserDtos.UserView;

public interface UserService {

    PageResult<UserView> list(String q, Integer page, Integer size);

    UserView get(Long id);

    UserView create(CreateUserRequest request);

    UserView update(Long id, UpdateUserRequest request);

    UserView setActive(Long id, boolean active);

    void resetPassword(Long id, String newPassword);

    void setPin(Long id, String pin);

    void changeOwnPassword(ChangePasswordRequest request);

    void forceSignOut(Long id);

    List<String> roles();
}
