package com.pos.purchasing.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.purchasing.domain.GoodsReceipt;
import com.pos.purchasing.domain.PoLine;
import com.pos.purchasing.domain.PurchaseOrder;

@Mapper
public interface PurchaseOrderMapper {

    long nextNumber();

    void insert(PurchaseOrder po);

    PurchaseOrder findById(@Param("id") Long id);

    PurchaseOrder lock(@Param("id") Long id);

    List<PurchaseOrder> search(@Param("storeId") Long storeId,
                               @Param("status") String status,
                               @Param("supplierId") Long supplierId,
                               @Param("size") int size,
                               @Param("offset") int offset);

    long count(@Param("storeId") Long storeId, @Param("status") String status, @Param("supplierId") Long supplierId);

    void updateDraft(PurchaseOrder po);

    void markOrdered(@Param("id") Long id);

    void setStatus(@Param("id") Long id, @Param("status") String status);

    void insertLine(PoLine line);

    void deleteLines(@Param("poId") Long poId);

    List<PoLine> lines(@Param("poId") Long poId);

    List<PoLine> lockLines(@Param("poId") Long poId);

    void addReceived(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    void insertReceipt(GoodsReceipt receipt);

    void insertReceiptLine(@Param("receiptId") Long receiptId,
                           @Param("poLineId") Long poLineId,
                           @Param("productId") Long productId,
                           @Param("quantity") BigDecimal quantity,
                           @Param("unitCost") BigDecimal unitCost);

    List<GoodsReceipt> receipts(@Param("poId") Long poId);
}
