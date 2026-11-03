package com.pos.inventory.service;

import java.math.BigDecimal;

import com.pos.common.dto.PageResult;
import com.pos.inventory.dto.MovementType;
import com.pos.inventory.dto.StockLevel;
import com.pos.inventory.dto.StockMovement;
import com.pos.inventory.dto.InventoryDtos.AdjustRequest;
import com.pos.inventory.dto.InventoryDtos.ReceiveRequest;
import com.pos.inventory.dto.InventoryDtos.TransferRequest;

public interface StockService {

    StockLevel apply(Long storeId, Long productId, BigDecimal delta, MovementType type, String refType, String refId, String note, Long userId, boolean allowNegative);

    StockLevel receive(ReceiveRequest request);

    StockLevel adjust(AdjustRequest request);

    void transfer(TransferRequest request);

    PageResult<StockLevel> search(Long storeId, Long productId, boolean lowOnly, Integer page, Integer size);

    StockLevel get(Long storeId, Long productId);

    StockLevel setReorderLevel(Long storeId, Long productId, BigDecimal reorderLevel);

    PageResult<StockMovement> movements(Long storeId, Long productId, Integer page, Integer size);
}
