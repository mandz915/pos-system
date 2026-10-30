package com.pos.sales.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.event.SaleCompletedEvent;
import com.pos.common.event.SaleRefundedEvent;
import com.pos.common.event.Topics;
import com.pos.common.model.PaymentMethod;
import com.pos.common.outbox.OutboxService;
import com.pos.sales.client.PaymentClient.PaymentResult;
import com.pos.sales.domain.Sale;
import com.pos.sales.domain.SaleLine;
import com.pos.sales.domain.SalePayment;
import com.pos.sales.domain.SaleReturn;
import com.pos.sales.domain.SaleReturnLine;
import com.pos.sales.domain.SaleStatus;
import com.pos.sales.domain.Shift;
import com.pos.sales.mapper.ReturnMapper;
import com.pos.sales.mapper.SaleLineMapper;
import com.pos.sales.mapper.SaleMapper;
import com.pos.sales.mapper.SalePaymentMapper;
import com.pos.sales.service.PricingCalculator.Totals;
import com.pos.sales.service.TenderPlanner.Plan;
import com.pos.sales.service.TenderPlanner.Tender;
import com.pos.sales.web.SalesDtos.CreateSaleRequest;

import lombok.RequiredArgsConstructor;

/**
 * The database transactions of the sale lifecycle. Calls to other services are NOT made here: they happen in
 * {@link SaleService} so no database lock is held while waiting for the network.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SaleTxService {

    /** One returned line: the line, how many units come back and the money credited for them. */
    public record RefundItem(SaleLine line, BigDecimal quantity, BigDecimal amount) {
    }

    private final SaleMapper sales;
    private final SaleLineMapper lineMapper;
    private final SalePaymentMapper paymentMapper;
    private final ReturnMapper returns;
    private final OutboxService outbox;

    public Long createDraft(CreateSaleRequest request, Shift shift, Long cashierId, List<SaleLine> lines) {
        Sale sale = new Sale();
        sale.setClientRef(request.clientRef());
        sale.setStoreId(request.storeId());
        sale.setTerminalId(request.terminalId());
        sale.setShiftId(shift.getId());
        sale.setCashierId(cashierId);
        sale.setCustomerId(request.customerId());
        sale.setStatus(SaleStatus.OPEN);
        sales.insert(sale);
        writeLines(sale.getId(), lines);
        return sale.getId();
    }

    public void replaceLines(Long saleId, List<SaleLine> lines) {
        Sale sale = requireLocked(saleId);
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw ApiException.conflict("Only an open sale can be edited (status " + sale.getStatus() + ")");
        }
        lineMapper.deleteBySale(saleId);
        writeLines(saleId, lines);
    }

    private void writeLines(Long saleId, List<SaleLine> lines) {
        int lineNo = 1;
        for (SaleLine line : lines) {
            line.setSaleId(saleId);
            line.setLineNo(lineNo++);
            lineMapper.insert(line);
        }
        Totals totals = PricingCalculator.totals(lines);
        Sale update = new Sale();
        update.setId(saleId);
        update.setSubtotal(totals.subtotal());
        update.setDiscountTotal(totals.discountTotal());
        update.setTaxTotal(totals.taxTotal());
        update.setTotal(totals.total());
        sales.updateTotals(update);
    }

    public void transition(Long saleId, SaleStatus from, SaleStatus to) {
        Sale sale = requireLocked(saleId);
        if (sale.getStatus() != from) {
            throw ApiException.conflict("Sale is " + sale.getStatus() + ", expected " + from);
        }
        sales.updateStatus(saleId, to);
    }

    public void voidSale(Long saleId, String reason) {
        Sale sale = requireLocked(saleId);
        if (sale.getStatus() != SaleStatus.OPEN && sale.getStatus() != SaleStatus.HELD) {
            throw ApiException.conflict("Only an unpaid sale can be voided; use a refund for completed sales");
        }
        sales.voidSale(saleId, reason);
    }

    /** Marks the sale paid: receipt number, payment rows and the SaleCompleted event in one transaction. */
    public void complete(Long saleId, Plan plan, List<PaymentResult> results) {
        Sale sale = requireLocked(saleId);
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw ApiException.conflict("Sale is " + sale.getStatus() + " and cannot be checked out");
        }
        if (sale.getTotal().compareTo(plan.total()) != 0) {
            throw ApiException.conflict("The sale changed while it was being paid; please retry");
        }
        long sequence = sales.nextReceiptNo(sale.getStoreId());
        String receiptNo = String.format("%03d-%08d", sale.getStoreId(), sequence);
        Instant now = Instant.now();
        sales.complete(saleId, receiptNo, plan.paidTotal(), plan.change(), now);

        for (int i = 0; i < plan.tenders().size(); i++) {
            Tender tender = plan.tenders().get(i);
            PaymentResult result = results.get(i);
            SalePayment payment = new SalePayment();
            payment.setSaleId(saleId);
            payment.setMethod(tender.method());
            payment.setAmount(tender.applied());
            payment.setTendered(tender.tendered());
            payment.setReference(tender.reference());
            payment.setPaymentId(result.id());
            payment.setGatewayRef(result.gatewayRef());
            paymentMapper.insert(payment);
        }

        List<SaleCompletedEvent.Line> eventLines = lineMapper.listBySale(saleId).stream()
                .map(l -> new SaleCompletedEvent.Line(l.getProductId(), l.getSku(), l.getName(), l.getQuantity(),
                        l.getUnitPrice(), l.getTaxAmount(), l.getLineTotal()))
                .toList();
        outbox.enqueue(Topics.SALE_COMPLETED, String.valueOf(saleId), new SaleCompletedEvent(
                UUID.randomUUID().toString(), saleId, receiptNo, sale.getStoreId(), sale.getTerminalId(),
                sale.getCashierId(), sale.getCustomerId(), sale.getSubtotal(), sale.getDiscountTotal(),
                sale.getTaxTotal(), sale.getTotal(), now, eventLines));
    }

    /** Records returned goods; flips the sale to REFUNDED once every unit has come back. */
    public Long recordRefund(Long saleId, Long shiftId, List<RefundItem> items, BigDecimal refundTotal,
                             PaymentMethod method, String reason, boolean restock, Long userId) {
        Sale sale = requireLocked(saleId);
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw ApiException.conflict("Only completed sales can be refunded (status " + sale.getStatus() + ")");
        }
        // Re-check the remaining quantities under the lock: another refund may have run meanwhile.
        Map<Long, SaleLine> current = new HashMap<>();
        for (SaleLine line : lineMapper.listBySale(saleId)) {
            current.put(line.getId(), line);
        }
        for (RefundItem item : items) {
            SaleLine line = current.get(item.line().getId());
            BigDecimal remaining = line.getQuantity().subtract(line.getReturnedQty());
            if (item.quantity().compareTo(remaining) > 0) {
                throw ApiException.conflict("Only " + remaining.stripTrailingZeros().toPlainString()
                        + " of '" + line.getName() + "' can still be returned");
            }
        }

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setSaleId(saleId);
        saleReturn.setShiftId(shiftId);
        saleReturn.setRefundTotal(refundTotal);
        saleReturn.setMethod(method);
        saleReturn.setReason(reason);
        saleReturn.setRestock(restock);
        saleReturn.setCreatedBy(userId);
        returns.insert(saleReturn);

        for (RefundItem item : items) {
            SaleReturnLine line = new SaleReturnLine();
            line.setReturnId(saleReturn.getId());
            line.setSaleLineId(item.line().getId());
            line.setProductId(item.line().getProductId());
            line.setQuantity(item.quantity());
            line.setAmount(item.amount());
            returns.insertLine(line);
            lineMapper.addReturned(item.line().getId(), item.quantity());
        }
        if (lineMapper.countNotFullyReturned(saleId) == 0) {
            sales.updateStatus(saleId, SaleStatus.REFUNDED);
        }

        List<SaleRefundedEvent.Line> eventLines = items.stream()
                .map(i -> new SaleRefundedEvent.Line(i.line().getProductId(), i.line().getSku(), i.line().getName(),
                        i.quantity(), i.amount()))
                .toList();
        outbox.enqueue(Topics.SALE_REFUNDED, String.valueOf(saleId), new SaleRefundedEvent(
                UUID.randomUUID().toString(), saleId, saleReturn.getId(), sale.getReceiptNo(), sale.getStoreId(),
                sale.getCustomerId(), refundTotal, restock, Instant.now(), eventLines));
        return saleReturn.getId();
    }

    private Sale requireLocked(Long saleId) {
        Sale sale = sales.lock(saleId);
        if (sale == null) {
            throw ApiException.notFound("Sale", saleId);
        }
        return sale;
    }
}
