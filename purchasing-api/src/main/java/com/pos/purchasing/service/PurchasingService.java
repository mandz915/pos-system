package com.pos.purchasing.service;

import java.util.List;

import com.pos.common.dto.PageResult;
import com.pos.purchasing.dto.PurchaseOrder;
import com.pos.purchasing.dto.Supplier;
import com.pos.purchasing.dto.PurchasingDtos.CreatePoRequest;
import com.pos.purchasing.dto.PurchasingDtos.PoView;
import com.pos.purchasing.dto.PurchasingDtos.ReceiveRequest;
import com.pos.purchasing.dto.PurchasingDtos.SupplierRequest;
import com.pos.purchasing.dto.PurchasingDtos.UpdatePoRequest;

public interface PurchasingService {

    List<Supplier> listSuppliers(String q);

    Supplier getSupplier(Long id);

    Supplier createSupplier(SupplierRequest request);

    Supplier updateSupplier(Long id, SupplierRequest request);

    Supplier setSupplierActive(Long id, boolean active);

    PoView create(CreatePoRequest request);

    PoView update(Long id, UpdatePoRequest request);

    PoView place(Long id);

    PoView cancel(Long id);

    PoView receive(Long id, ReceiveRequest request);

    PoView view(Long id);

    PageResult<PurchaseOrder> search(Long storeId, String status, Long supplierId, Integer page, Integer size);
}
