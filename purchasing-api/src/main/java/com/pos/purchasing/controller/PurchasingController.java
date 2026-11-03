package com.pos.purchasing.controller;

import java.util.List;

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
import com.pos.common.dto.ActiveRequest;
import com.pos.common.dto.PageResult;
import com.pos.purchasing.dto.PurchaseOrder;
import com.pos.purchasing.dto.Supplier;
import com.pos.purchasing.service.PurchasingService;
import com.pos.purchasing.dto.PurchasingDtos.CreatePoRequest;
import com.pos.purchasing.dto.PurchasingDtos.PoView;
import com.pos.purchasing.dto.PurchasingDtos.ReceiveRequest;
import com.pos.purchasing.dto.PurchasingDtos.SupplierRequest;
import com.pos.purchasing.dto.PurchasingDtos.UpdatePoRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/purchasing")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
public class PurchasingController {

    private final PurchasingService service;

    @GetMapping("/suppliers")
    public List<Supplier> suppliers(@RequestParam(required = false) String q) {
        return service.listSuppliers(q);
    }

    @GetMapping("/suppliers/{id}")
    public Supplier supplier(@PathVariable Long id) {
        return service.getSupplier(id);
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Supplier createSupplier(@Valid @RequestBody SupplierRequest request) {
        return service.createSupplier(request);
    }

    @PutMapping("/suppliers/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Supplier updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return service.updateSupplier(id, request);
    }

    @PatchMapping("/suppliers/{id}/active")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Supplier setSupplierActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return service.setSupplierActive(id, request.active());
    }

    @GetMapping("/orders")
    public PageResult<PurchaseOrder> orders(@RequestParam(required = false) Long storeId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long supplierId,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return service.search(storeId, status, supplierId, page, size);
    }

    @GetMapping("/orders/{id}")
    public PoView order(@PathVariable Long id) {
        return service.view(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PoView create(@Valid @RequestBody CreatePoRequest request) {
        return service.create(request);
    }

    @PutMapping("/orders/{id}")
    public PoView update(@PathVariable Long id, @Valid @RequestBody UpdatePoRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/orders/{id}/place")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public PoView place(@PathVariable Long id) {
        return service.place(id);
    }

    @PostMapping("/orders/{id}/cancel")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public PoView cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/orders/{id}/receive")
    public PoView receive(@PathVariable Long id, @Valid @RequestBody ReceiveRequest request) {
        return service.receive(id, request);
    }
}
