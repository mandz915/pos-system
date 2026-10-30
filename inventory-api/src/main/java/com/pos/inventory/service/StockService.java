package com.pos.inventory.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.event.LowStockEvent;
import com.pos.common.event.Topics;
import com.pos.common.outbox.OutboxService;
import com.pos.common.security.CurrentUser;
import com.pos.common.web.PageResult;
import com.pos.common.web.Paging;
import com.pos.inventory.domain.MovementType;
import com.pos.inventory.domain.StockLevel;
import com.pos.inventory.domain.StockMovement;
import com.pos.inventory.mapper.MovementMapper;
import com.pos.inventory.mapper.StockLevelMapper;
import com.pos.inventory.web.InventoryDtos.AdjustRequest;
import com.pos.inventory.web.InventoryDtos.ReceiveRequest;
import com.pos.inventory.web.InventoryDtos.TransferRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final StockLevelMapper levels;
    private final MovementMapper movements;
    private final OutboxService outbox;

    /**
     * The single place where stock changes. Locks the stock row, applies the signed delta, records a movement and
     * emits a low-stock event when the level crosses its reorder threshold.
     *
     * @param allowNegative sales must never be blocked at the till, so event-driven sale deductions may go below zero
     */
    public StockLevel apply(Long storeId, Long productId, BigDecimal delta, MovementType type,
                            String refType, String refId, String note, Long userId, boolean allowNegative) {
        levels.ensure(storeId, productId);
        StockLevel level = levels.lock(storeId, productId);
        BigDecimal before = level.getQuantity();
        BigDecimal after = before.add(delta);
        if (!allowNegative && after.signum() < 0) {
            throw ApiException.conflict("Insufficient stock for product " + productId + " in store " + storeId
                    + " (available " + before.stripTrailingZeros().toPlainString() + ")");
        }
        levels.updateQuantity(level.getId(), after);

        StockMovement movement = new StockMovement();
        movement.setStoreId(storeId);
        movement.setProductId(productId);
        movement.setType(type);
        movement.setQuantity(delta);
        movement.setBalanceAfter(after);
        movement.setRefType(refType);
        movement.setRefId(refId);
        movement.setNote(note);
        movement.setCreatedBy(userId);
        movements.insert(movement);

        BigDecimal reorder = level.getReorderLevel();
        boolean crossedThreshold = delta.signum() < 0
                && reorder.signum() > 0
                && before.compareTo(reorder) > 0
                && after.compareTo(reorder) <= 0;
        if (crossedThreshold) {
            outbox.enqueue(Topics.LOW_STOCK, storeId + ":" + productId,
                    new LowStockEvent(UUID.randomUUID().toString(), storeId, productId, after, reorder, Instant.now()));
        }
        level.setQuantity(after);
        return level;
    }

    public StockLevel receive(ReceiveRequest request) {
        CurrentUser.requireStoreAccess(request.storeId());
        return apply(request.storeId(), request.productId(), request.quantity(), MovementType.RECEIPT,
                "MANUAL", null, request.note(), CurrentUser.get().userId(), false);
    }

    public StockLevel adjust(AdjustRequest request) {
        CurrentUser.requireStoreAccess(request.storeId());
        if (request.delta().signum() == 0) {
            throw ApiException.badRequest("delta must not be zero");
        }
        return apply(request.storeId(), request.productId(), request.delta(), MovementType.ADJUSTMENT,
                "MANUAL", null, request.reason(), CurrentUser.get().userId(), false);
    }

    public void transfer(TransferRequest request) {
        if (request.fromStoreId().equals(request.toStoreId())) {
            throw ApiException.badRequest("Source and destination stores must differ");
        }
        CurrentUser.requireStoreAccess(request.fromStoreId());
        String ref = UUID.randomUUID().toString();
        Long userId = CurrentUser.get().userId();

        record Op(Long storeId, BigDecimal delta, MovementType type) {
        }
        List<Op> ops = new ArrayList<>();
        ops.add(new Op(request.fromStoreId(), request.quantity().negate(), MovementType.TRANSFER_OUT));
        ops.add(new Op(request.toStoreId(), request.quantity(), MovementType.TRANSFER_IN));
        // Always lock rows in the same order so concurrent opposite transfers cannot deadlock.
        ops.sort(Comparator.comparing(Op::storeId));
        for (Op op : ops) {
            apply(op.storeId(), request.productId(), op.delta(), op.type(), "TRANSFER", ref, request.note(), userId, false);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<StockLevel> search(Long storeId, Long productId, boolean lowOnly, Integer page, Integer size) {
        Long store = CurrentUser.effectiveStoreId(storeId);
        Paging paging = Paging.of(page, size);
        return PageResult.of(levels.search(store, productId, lowOnly, paging.size(), paging.offset()), paging,
                levels.count(store, productId, lowOnly));
    }

    @Transactional(readOnly = true)
    public StockLevel get(Long storeId, Long productId) {
        CurrentUser.requireStoreAccess(storeId);
        StockLevel level = levels.find(storeId, productId);
        if (level == null) {
            throw ApiException.notFound("Stock record for product", productId);
        }
        return level;
    }

    public StockLevel setReorderLevel(Long storeId, Long productId, BigDecimal reorderLevel) {
        CurrentUser.requireStoreAccess(storeId);
        levels.ensure(storeId, productId);
        levels.updateReorderLevel(storeId, productId, reorderLevel);
        return levels.find(storeId, productId);
    }

    @Transactional(readOnly = true)
    public PageResult<StockMovement> movements(Long storeId, Long productId, Integer page, Integer size) {
        Long store = CurrentUser.effectiveStoreId(storeId);
        Paging paging = Paging.of(page, size);
        return PageResult.of(movements.search(store, productId, paging.size(), paging.offset()), paging,
                movements.count(store, productId));
    }
}
