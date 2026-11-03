package com.pos.inventory.service;

import java.util.List;

import com.pos.inventory.dto.StockCount;
import com.pos.inventory.dto.InventoryDtos.CountLineRequest;
import com.pos.inventory.dto.InventoryDtos.CountView;

public interface StockCountService {

    CountView open(Long storeId);

    List<StockCount> list(Long storeId);

    CountView view(Long id);

    CountView submitLines(Long id, List<CountLineRequest> lines);

    CountView post(Long id);
}
