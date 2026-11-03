package com.pos.administration.controller;

import java.util.List;
import java.util.Map;

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

import com.pos.administration.dto.Store;
import com.pos.administration.dto.Terminal;
import com.pos.administration.service.AdminService;
import com.pos.administration.dto.AdminDtos.SettingRequest;
import com.pos.administration.dto.AdminDtos.StoreRequest;
import com.pos.administration.dto.AdminDtos.TerminalRequest;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.dto.ActiveRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService admin;

    @GetMapping("/stores")
    public List<Store> stores() {
        return admin.listStores();
    }

    @GetMapping("/stores/{id}")
    public Store store(@PathVariable Long id) {
        return admin.getStore(id);
    }

    @PostMapping("/stores")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(Roles.ADMIN)
    public Store createStore(@Valid @RequestBody StoreRequest request) {
        return admin.createStore(request);
    }

    @PutMapping("/stores/{id}")
    @RequireRole(Roles.ADMIN)
    public Store updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return admin.updateStore(id, request);
    }

    @PatchMapping("/stores/{id}/active")
    @RequireRole(Roles.ADMIN)
    public Store setStoreActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return admin.setStoreActive(id, request.active());
    }

    @GetMapping("/terminals")
    public List<Terminal> terminals(@RequestParam(required = false) Long storeId) {
        return admin.listTerminals(storeId);
    }

    @GetMapping("/terminals/{id}")
    public Terminal terminal(@PathVariable Long id) {
        Terminal terminal = admin.getTerminal(id);
        CurrentUser.requireStoreAccess(terminal.getStoreId());
        return terminal;
    }

    @PostMapping("/terminals")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(Roles.ADMIN)
    public Terminal createTerminal(@Valid @RequestBody TerminalRequest request) {
        return admin.createTerminal(request);
    }

    @PutMapping("/terminals/{id}")
    @RequireRole(Roles.ADMIN)
    public Terminal updateTerminal(@PathVariable Long id, @Valid @RequestBody TerminalRequest request) {
        return admin.updateTerminal(id, request);
    }

    @PatchMapping("/terminals/{id}/active")
    @RequireRole(Roles.ADMIN)
    public Terminal setTerminalActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return admin.setTerminalActive(id, request.active());
    }

    @PostMapping("/terminals/{id}/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@PathVariable Long id) {
        admin.heartbeat(id);
    }

    @GetMapping("/settings")
    public Map<String, String> settings(@RequestParam(required = false) Long storeId) {
        CurrentUser.requireStoreAccess(storeId);
        return admin.effectiveSettings(storeId);
    }

    @PutMapping("/settings")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Map<String, String> saveSetting(@Valid @RequestBody SettingRequest request) {
        if (request.storeId() == null) {
            CurrentUser.requireAnyRole(Roles.ADMIN);
        } else {
            CurrentUser.requireStoreAccess(request.storeId());
        }
        return admin.saveSetting(request);
    }
}
