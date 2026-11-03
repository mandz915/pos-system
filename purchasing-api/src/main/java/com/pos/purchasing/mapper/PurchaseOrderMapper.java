package com.pos.purchasing.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.purchasing.dto.GoodsReceipt;
import com.pos.purchasing.dto.PoLine;
import com.pos.purchasing.dto.PurchaseOrder;

@Mapper
public interface PurchaseOrderMapper {

    long nextNumber();

    int insert(PurchaseOrder po);

    PurchaseOrder findById(@Param("id") Long id);

    PurchaseOrder lock(@Param("id") Long id);

    List<PurchaseOrder> search(@Param("storeId") Long storeId,
                               @Param("status") String status,
                               @Param("supplierId") Long supplierId,
                               @Param("size") int size,
                               @Param("offset") int offset);

    long count(@Param("storeId") Long storeId, @Param("status") String status, @Param("supplierId") Long supplierId);

    int updateDraft(PurchaseOrder po);

    int markOrdered(@Param("id") Long id);

    int setStatus(@Param("id") Long id, @Param("status") String status);

    int insertLine(PoLine line);

    int deleteLines(@Param("poId") Long poId);

    List<PoLine> lines(@Param("poId") Long poId);

    List<PoLine> lockLines(@Param("poId") Long poId);

    int addReceived(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    int insertReceipt(GoodsReceipt receipt);

    int insertReceiptLine(@Param("receiptId") Long receiptId,
                          @Param("poLineId") Long poLineId,
                          @Param("productId") Long productId,
                          @Param("quantity") BigDecimal quantity,
                          @Param("unitCost") BigDecimal unitCost);

    List<GoodsReceipt> receipts(@Param("poId") Long poId);
}
