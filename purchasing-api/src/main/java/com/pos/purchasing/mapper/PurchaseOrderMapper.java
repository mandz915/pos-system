package com.pos.purchasing.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.purchasing.domain.GoodsReceipt;
import com.pos.purchasing.domain.PoLine;
import com.pos.purchasing.domain.PurchaseOrder;

@Mapper
public interface PurchaseOrderMapper {

    @Select("SELECT nextval('po_number_seq')")
    long nextNumber();

    @Insert("""
            INSERT INTO purchase_orders (po_number, supplier_id, store_id, status, note, created_by)
            VALUES (#{poNumber}, #{supplierId}, #{storeId}, #{status}, #{note}, #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PurchaseOrder po);

    @Select("SELECT * FROM purchase_orders WHERE id = #{id}")
    PurchaseOrder findById(@Param("id") Long id);

    @Select("SELECT * FROM purchase_orders WHERE id = #{id} FOR UPDATE")
    PurchaseOrder lock(@Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM purchase_orders
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="supplierId != null">AND supplier_id = #{supplierId}</if>
            </where>
            ORDER BY id DESC LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<PurchaseOrder> search(@Param("storeId") Long storeId,
                               @Param("status") String status,
                               @Param("supplierId") Long supplierId,
                               @Param("size") int size,
                               @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM purchase_orders
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="supplierId != null">AND supplier_id = #{supplierId}</if>
            </where>
            </script>
            """)
    long count(@Param("storeId") Long storeId, @Param("status") String status, @Param("supplierId") Long supplierId);

    @Update("UPDATE purchase_orders SET note = #{note}, total_cost = #{totalCost} WHERE id = #{id}")
    int updateDraft(PurchaseOrder po);

    @Update("UPDATE purchase_orders SET status = 'ORDERED', ordered_at = now() WHERE id = #{id}")
    int markOrdered(@Param("id") Long id);

    @Update("UPDATE purchase_orders SET status = #{status}, closed_at = CASE WHEN #{status} IN ('RECEIVED', 'CANCELLED') THEN now() ELSE closed_at END WHERE id = #{id}")
    int setStatus(@Param("id") Long id, @Param("status") String status);

    // ---- lines
    @Insert("INSERT INTO po_lines (po_id, product_id, ordered_qty, unit_cost) VALUES (#{poId}, #{productId}, #{orderedQty}, #{unitCost})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertLine(PoLine line);

    @Delete("DELETE FROM po_lines WHERE po_id = #{poId}")
    int deleteLines(@Param("poId") Long poId);

    @Select("SELECT * FROM po_lines WHERE po_id = #{poId} ORDER BY id")
    List<PoLine> lines(@Param("poId") Long poId);

    @Select("SELECT * FROM po_lines WHERE po_id = #{poId} FOR UPDATE")
    List<PoLine> lockLines(@Param("poId") Long poId);

    @Update("UPDATE po_lines SET received_qty = received_qty + #{quantity} WHERE id = #{id}")
    int addReceived(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    // ---- receipts
    @Insert("INSERT INTO goods_receipts (po_id, note, received_by) VALUES (#{poId}, #{note}, #{receivedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertReceipt(GoodsReceipt receipt);

    @Insert("""
            INSERT INTO goods_receipt_lines (receipt_id, po_line_id, product_id, quantity, unit_cost)
            VALUES (#{receiptId}, #{poLineId}, #{productId}, #{quantity}, #{unitCost})
            """)
    int insertReceiptLine(@Param("receiptId") Long receiptId,
                          @Param("poLineId") Long poLineId,
                          @Param("productId") Long productId,
                          @Param("quantity") BigDecimal quantity,
                          @Param("unitCost") BigDecimal unitCost);

    @Select("SELECT * FROM goods_receipts WHERE po_id = #{poId} ORDER BY id")
    List<GoodsReceipt> receipts(@Param("poId") Long poId);
}
