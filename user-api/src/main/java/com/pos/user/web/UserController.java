package com.pos.user.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.web.ActiveRequest;
import com.pos.common.web.PageResult;
import com.pos.user.service.UserService;
import com.pos.user.web.UserDtos.ChangePasswordRequest;
import com.pos.user.web.UserDtos.CreateUserRequest;
import com.pos.user.web.UserDtos.ResetPasswordRequest;
import com.pos.user.web.UserDtos.SetPinRequest;
import com.pos.user.web.UserDtos.UpdateUserRequest;
import com.pos.user.web.UserDtos.UserView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public PageResult<UserView> list(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size) {
        return userService.list(q, page, size);
    }

    @GetMapping("/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public UserView get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public UserView create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public UserView update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public UserView setActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return userService.setActive(id, request.active());
    }

    @PutMapping("/{id}/password")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
    }

    @PutMapping("/{id}/pin")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPin(@PathVariable Long id, @Valid @RequestBody SetPinRequest request) {
        userService.setPin(id, request.pin());
    }

    /** Any signed-in user can change their own password. */
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(request);
    }
}
