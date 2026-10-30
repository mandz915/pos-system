package com.pos.inventory.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.pos.common.web.PageResult;
import com.pos.inventory.domain.StockCount;
import com.pos.inventory.domain.StockLevel;
import com.pos.inventory.domain.StockMovement;
import com.pos.inventory.service.StockCountService;
import com.pos.inventory.service.StockService;
import com.pos.inventory.web.InventoryDtos.AdjustRequest;
import com.pos.inventory.web.InventoryDtos.CountLinesRequest;
import com.pos.inventory.web.InventoryDtos.CountView;
import com.pos.inventory.web.InventoryDtos.OpenCountRequest;
import com.pos.inventory.web.InventoryDtos.ReceiveRequest;
import com.pos.inventory.web.InventoryDtos.ReorderLevelRequest;
import com.pos.inventory.web.InventoryDtos.TransferRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Stock is queried by any signed-in user (limited to their store); changes need supervisor level or above.
 * Sales and purchase receipts update stock through Kafka events, not through this API.
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final StockService stock;
    private final StockCountService counts;

    @GetMapping("/stock")
    public PageResult<StockLevel> stock(@RequestParam(required = false) Long storeId,
                                        @RequestParam(required = false) Long productId,
                                        @RequestParam(defaultValue = "false") boolean lowOnly,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return stock.search(storeId, productId, lowOnly, page, size);
    }

    @GetMapping("/stock/{storeId}/{productId}")
    public StockLevel level(@PathVariable Long storeId, @PathVariable Long productId) {
        return stock.get(storeId, productId);
    }

    @PutMapping("/stock/reorder-level")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public StockLevel setReorderLevel(@Valid @RequestBody ReorderLevelRequest request) {
        return stock.setReorderLevel(request.storeId(), request.productId(), request.reorderLevel());
    }

    @PostMapping("/receive")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public StockLevel receive(@Valid @RequestBody ReceiveRequest request) {
        return stock.receive(request);
    }

    @PostMapping("/adjust")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public StockLevel adjust(@Valid @RequestBody AdjustRequest request) {
        return stock.adjust(request);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public void transfer(@Valid @RequestBody TransferRequest request) {
        stock.transfer(request);
    }

    @GetMapping("/movements")
    public PageResult<StockMovement> movements(@RequestParam(required = false) Long storeId,
                                               @RequestParam(required = false) Long productId,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return stock.movements(storeId, productId, page, size);
    }

    // ---- physical counts
    @GetMapping("/counts")
    public List<StockCount> counts(@RequestParam(required = false) Long storeId) {
        return counts.list(storeId);
    }

    @PostMapping("/counts")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public CountView openCount(@Valid @RequestBody OpenCountRequest request) {
        return counts.open(request.storeId());
    }

    @GetMapping("/counts/{id}")
    public CountView count(@PathVariable Long id) {
        return counts.view(id);
    }

    @PutMapping("/counts/{id}/lines")
    @RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
    public CountView submitLines(@PathVariable Long id, @Valid @RequestBody CountLinesRequest request) {
        return counts.submitLines(id, request.lines());
    }

    @PostMapping("/counts/{id}/post")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public CountView post(@PathVariable Long id) {
        return counts.post(id);
    }
}
