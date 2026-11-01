package com.pos.inventory.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.security.CurrentUser;
import com.pos.inventory.domain.MovementType;
import com.pos.inventory.domain.StockCount;
import com.pos.inventory.domain.StockCountLine;
import com.pos.inventory.domain.StockLevel;
import com.pos.inventory.mapper.StockCountMapper;
import com.pos.inventory.mapper.StockLevelMapper;
import com.pos.inventory.dto.InventoryDtos.CountLineRequest;
import com.pos.inventory.dto.InventoryDtos.CountView;

import lombok.RequiredArgsConstructor;

/** Physical stock counts: record counted quantities, then post the differences as COUNT movements. */
@Service
@RequiredArgsConstructor
@Transactional
public class StockCountService {

    private final StockCountMapper counts;
    private final StockLevelMapper levels;
    private final StockService stock;

    public CountView open(Long storeId) {
        CurrentUser.requireStoreAccess(storeId);
        StockCount count = new StockCount();
        count.setStoreId(storeId);
        count.setCreatedBy(CurrentUser.get().userId());
        counts.insert(count);
        return view(count.getId());
    }

    @Transactional(readOnly = true)
    public List<StockCount> list(Long storeId) {
        return counts.findAll(CurrentUser.effectiveStoreId(storeId));
    }

    @Transactional(readOnly = true)
    public CountView view(Long id) {
        StockCount count = counts.findById(id);
        if (count == null) {
            throw ApiException.notFound("Stock count", id);
        }
        CurrentUser.requireStoreAccess(count.getStoreId());
        return new CountView(count, counts.lines(id));
    }

    public CountView submitLines(Long id, List<CountLineRequest> lines) {
        StockCount count = counts.lock(id);
        if (count == null) {
            throw ApiException.notFound("Stock count", id);
        }
        CurrentUser.requireStoreAccess(count.getStoreId());
        if (!"OPEN".equals(count.getStatus())) {
            throw ApiException.conflict("Stock count " + id + " is already posted");
        }
        for (CountLineRequest line : lines) {
            counts.upsertLine(id, line.productId(), line.countedQty());
        }
        return view(id);
    }

    public CountView post(Long id) {
        StockCount count = counts.lock(id);
        if (count == null) {
            throw ApiException.notFound("Stock count", id);
        }
        CurrentUser.requireStoreAccess(count.getStoreId());
        if (!"OPEN".equals(count.getStatus())) {
            throw ApiException.conflict("Stock count " + id + " is already posted");
        }
        Long userId = CurrentUser.get().userId();
        for (StockCountLine line : counts.lines(id)) {
            levels.ensure(count.getStoreId(), line.getProductId());
            StockLevel current = levels.lock(count.getStoreId(), line.getProductId());
            BigDecimal delta = line.getCountedQty().subtract(current.getQuantity());
            counts.setSystemQty(line.getId(), current.getQuantity());
            if (delta.signum() != 0) {
                stock.apply(count.getStoreId(), line.getProductId(), delta, MovementType.COUNT,
                        "COUNT", String.valueOf(id), "Stock count #" + id, userId, true);
            }
        }
        counts.markPosted(id);
        return view(id);
    }
}
