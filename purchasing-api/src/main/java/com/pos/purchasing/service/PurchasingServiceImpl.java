package com.pos.purchasing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.event.GoodsReceivedEvent;
import com.pos.common.event.Topics;
import com.pos.common.outbox.OutboxService;
import com.pos.common.security.CurrentUser;
import com.pos.common.util.Money;
import com.pos.common.dto.PageResult;
import com.pos.common.dto.Paging;
import com.pos.purchasing.dto.GoodsReceipt;
import com.pos.purchasing.dto.PoLine;
import com.pos.purchasing.dto.PoStatus;
import com.pos.purchasing.dto.PurchaseOrder;
import com.pos.purchasing.dto.Supplier;
import com.pos.purchasing.mapper.PurchaseOrderMapper;
import com.pos.purchasing.mapper.SupplierMapper;
import com.pos.purchasing.dto.PurchasingDtos.CreatePoRequest;
import com.pos.purchasing.dto.PurchasingDtos.PoLineRequest;
import com.pos.purchasing.dto.PurchasingDtos.PoView;
import com.pos.purchasing.dto.PurchasingDtos.ReceiveLine;
import com.pos.purchasing.dto.PurchasingDtos.ReceiveRequest;
import com.pos.purchasing.dto.PurchasingDtos.SupplierRequest;
import com.pos.purchasing.dto.PurchasingDtos.UpdatePoRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchasingServiceImpl implements PurchasingService {

    private final SupplierMapper suppliers;
    private final PurchaseOrderMapper orders;
    private final OutboxService outbox;

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers(String q) {
        return suppliers.findAll(q == null || q.isBlank() ? null : "%" + q.trim() + "%");
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier getSupplier(Long id) {
        Supplier supplier = suppliers.findById(id);
        if (supplier == null) {
            throw ApiException.notFound("Supplier", id);
        }
        return supplier;
    }

    @Override
    public Supplier createSupplier(SupplierRequest request) {
        Supplier supplier = new Supplier();
        apply(supplier, request);
        supplier.setCode(request.code() == null || request.code().isBlank()
                ? "S" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : request.code().trim().toUpperCase());
        suppliers.insert(supplier);
        return getSupplier(supplier.getId());
    }

    @Override
    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = getSupplier(id);
        apply(supplier, request);
        suppliers.update(supplier);
        return getSupplier(id);
    }

    @Override
    public Supplier setSupplierActive(Long id, boolean active) {
        getSupplier(id);
        suppliers.setActive(id, active);
        return getSupplier(id);
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setContact(request.contact());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
    }

    @Override
    public PoView create(CreatePoRequest request) {
        CurrentUser.requireStoreAccess(request.storeId());
        Supplier supplier = getSupplier(request.supplierId());
        if (!supplier.isActive()) {
            throw ApiException.conflict("Supplier " + supplier.getName() + " is inactive");
        }
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(String.format("PO-%06d", orders.nextNumber()));
        po.setSupplierId(request.supplierId());
        po.setStoreId(request.storeId());
        po.setStatus(PoStatus.DRAFT);
        po.setNote(request.note());
        po.setCreatedBy(CurrentUser.get().userId());
        orders.insert(po);
        writeLines(po, request.lines());
        return view(po.getId());
    }

    @Override
    public PoView update(Long id, UpdatePoRequest request) {
        PurchaseOrder po = lockOrder(id);
        if (po.getStatus() != PoStatus.DRAFT) {
            throw ApiException.conflict("Only a draft purchase order can be edited (status " + po.getStatus() + ")");
        }
        po.setNote(request.note());
        orders.deleteLines(id);
        writeLines(po, request.lines());
        return view(id);
    }

    private void writeLines(PurchaseOrder po, List<PoLineRequest> requests) {
        Set<Long> seen = new HashSet<>();
        BigDecimal total = Money.zero();
        for (PoLineRequest request : requests) {
            if (!seen.add(request.productId())) {
                throw ApiException.badRequest("Product " + request.productId() + " appears twice; merge the lines");
            }
            PoLine line = new PoLine();
            line.setPoId(po.getId());
            line.setProductId(request.productId());
            line.setOrderedQty(request.orderedQty());
            line.setUnitCost(Money.round(request.unitCost()));
            orders.insertLine(line);
            total = total.add(Money.round(line.getUnitCost().multiply(line.getOrderedQty())));
        }
        po.setTotalCost(total);
        orders.updateDraft(po);
    }

    @Override
    public PoView place(Long id) {
        PurchaseOrder po = lockOrder(id);
        if (po.getStatus() != PoStatus.DRAFT) {
            throw ApiException.conflict("Purchase order is " + po.getStatus() + ", expected DRAFT");
        }
        orders.markOrdered(id);
        return view(id);
    }

    @Override
    public PoView cancel(Long id) {
        PurchaseOrder po = lockOrder(id);
        if (!PoStatusRules.canCancel(po.getStatus())) {
            throw ApiException.conflict("A purchase order that is " + po.getStatus() + " cannot be cancelled");
        }
        orders.setStatus(id, PoStatus.CANCELLED.name());
        return view(id);
    }

    @Override
    public PoView receive(Long id, ReceiveRequest request) {
        PurchaseOrder po = lockOrder(id);
        if (!PoStatusRules.canReceive(po.getStatus())) {
            throw ApiException.conflict("Goods cannot be received on a purchase order that is " + po.getStatus());
        }
        Map<Long, PoLine> byId = orders.lockLines(id).stream()
                .collect(Collectors.toMap(PoLine::getId, Function.identity()));

        Set<Long> seen = new HashSet<>();
        for (ReceiveLine requested : request.lines()) {
            PoLine line = byId.get(requested.poLineId());
            if (line == null) {
                throw ApiException.badRequest("Line " + requested.poLineId() + " is not on " + po.getPoNumber());
            }
            if (!seen.add(requested.poLineId())) {
                throw ApiException.badRequest("Line " + requested.poLineId() + " is listed twice");
            }
            if (requested.quantity().compareTo(line.outstanding()) > 0) {
                throw ApiException.conflict("Only " + line.outstanding().stripTrailingZeros().toPlainString()
                        + " of product " + line.getProductId() + " is still outstanding");
            }
        }

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPoId(id);
        receipt.setNote(request.note());
        receipt.setReceivedBy(CurrentUser.get().userId());
        orders.insertReceipt(receipt);

        List<GoodsReceivedEvent.Line> eventLines = new java.util.ArrayList<>();
        for (ReceiveLine requested : request.lines()) {
            PoLine line = byId.get(requested.poLineId());
            orders.insertReceiptLine(receipt.getId(), line.getId(), line.getProductId(), requested.quantity(), line.getUnitCost());
            orders.addReceived(line.getId(), requested.quantity());
            line.setReceivedQty(line.getReceivedQty().add(requested.quantity()));
            eventLines.add(new GoodsReceivedEvent.Line(line.getProductId(), requested.quantity(), line.getUnitCost()));
        }
        orders.setStatus(id, PoStatusRules.afterReceiving(List.copyOf(byId.values())).name());

        outbox.enqueue(Topics.GOODS_RECEIVED, po.getPoNumber(), new GoodsReceivedEvent(
                UUID.randomUUID().toString(), id, po.getPoNumber(), receipt.getId(), po.getStoreId(),
                Instant.now(), eventLines));
        return view(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PoView view(Long id) {
        PurchaseOrder po = orders.findById(id);
        if (po == null) {
            throw ApiException.notFound("Purchase order", id);
        }
        CurrentUser.requireStoreAccess(po.getStoreId());
        return new PoView(po, orders.lines(id), orders.receipts(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PurchaseOrder> search(Long storeId, String status, Long supplierId, Integer page, Integer size) {
        Long store = CurrentUser.effectiveStoreId(storeId);
        String normalized = null;
        if (status != null && !status.isBlank()) {
            try {
                normalized = PoStatus.valueOf(status.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Unknown status '" + status + "'");
            }
        }
        Paging paging = Paging.of(page, size);
        return PageResult.of(orders.search(store, normalized, supplierId, paging.size(), paging.offset()), paging,
                orders.count(store, normalized, supplierId));
    }

    private PurchaseOrder lockOrder(Long id) {
        PurchaseOrder po = orders.lock(id);
        if (po == null) {
            throw ApiException.notFound("Purchase order", id);
        }
        CurrentUser.requireStoreAccess(po.getStoreId());
        return po;
    }
}
