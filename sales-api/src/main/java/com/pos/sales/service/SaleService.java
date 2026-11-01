package com.pos.sales.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pos.common.error.ApiException;
import com.pos.common.model.PaymentMethod;
import com.pos.common.pricing.PromotionEngine;
import com.pos.common.pricing.PromotionEngine.LineDiscount;
import com.pos.common.pricing.PromotionRule;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.common.util.Money;
import com.pos.common.dto.PageResult;
import com.pos.common.dto.Paging;
import com.pos.sales.client.CatalogClient;
import com.pos.sales.client.CatalogClient.ProductInfo;
import com.pos.sales.client.PaymentClient;
import com.pos.sales.client.PaymentClient.PaymentResult;
import com.pos.sales.domain.Sale;
import com.pos.sales.domain.SaleLine;
import com.pos.sales.domain.SaleStatus;
import com.pos.sales.domain.Shift;
import com.pos.sales.domain.ShiftStatus;
import com.pos.sales.mapper.ReturnMapper;
import com.pos.sales.mapper.SaleLineMapper;
import com.pos.sales.mapper.SaleMapper;
import com.pos.sales.mapper.SalePaymentMapper;
import com.pos.sales.mapper.ShiftMapper;
import com.pos.sales.service.PricingCalculator.LineAmounts;
import com.pos.sales.service.SaleTxService.RefundItem;
import com.pos.sales.service.TenderPlanner.Plan;
import com.pos.sales.service.TenderPlanner.Tender;
import com.pos.sales.dto.SalesDtos.CheckoutRequest;
import com.pos.sales.dto.SalesDtos.CreateSaleRequest;
import com.pos.sales.dto.SalesDtos.LineRequest;
import com.pos.sales.dto.SalesDtos.LineTax;
import com.pos.sales.dto.SalesDtos.RefundLine;
import com.pos.sales.dto.SalesDtos.RefundRequest;
import com.pos.sales.dto.SalesDtos.SaleView;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

/**
 * Orchestrates the sale lifecycle: calls other services first (prices, payments), then commits the result in a
 * single short transaction through {@link SaleTxService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {


    private final SaleMapper sales;
    private final SaleLineMapper lineMapper;
    private final SalePaymentMapper paymentMapper;
    private final ReturnMapper returns;
    private final ShiftMapper shifts;
    private final SaleTxService tx;
    private final CatalogClient catalog;
    private final PaymentClient payments;

    @Value("${pos.sales.timezone:Asia/Manila}")
    private String timezone;

    // ------------------------------------------------------------------ create / edit
    /** Idempotent on {@code clientRef}: repeating the request returns the sale created the first time. */
    public SaleView create(CreateSaleRequest request) {
        CurrentUser.requireStoreAccess(request.storeId());
        Sale existing = sales.findByClientRef(request.clientRef());
        if (existing != null) {
            return view(existing.getId());
        }
        Shift shift = shifts.findOpenByTerminal(request.terminalId());
        if (shift == null) {
            throw ApiException.conflict("No open shift on terminal " + request.terminalId() + ". Open a shift first.");
        }
        if (!shift.getStoreId().equals(request.storeId())) {
            throw ApiException.badRequest("Terminal " + request.terminalId() + " does not belong to store " + request.storeId());
        }
        List<SaleLine> lines = priceLines(request.lines(), request.storeId(), request.couponCodes());
        try {
            Long id = tx.createDraft(request, shift, CurrentUser.get().userId(), lines);
            return view(id);
        } catch (DuplicateKeyException e) {
            // Two requests with the same clientRef raced: return the winner.
            Sale winner = sales.findByClientRef(request.clientRef());
            if (winner == null) {
                throw e;
            }
            return view(winner.getId());
        }
    }

    public SaleView replaceLines(Long saleId, List<LineRequest> requests, List<String> couponCodes) {
        Sale sale = require(saleId);
        assertCanModify(sale);
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw ApiException.conflict("Only an open sale can be edited (status " + sale.getStatus() + ")");
        }
        tx.replaceLines(saleId, priceLines(requests, sale.getStoreId(), couponCodes));
        return view(saleId);
    }

    public SaleView hold(Long saleId) {
        assertCanModify(require(saleId));
        tx.transition(saleId, SaleStatus.OPEN, SaleStatus.HELD);
        return view(saleId);
    }

    public SaleView resume(Long saleId) {
        assertCanModify(require(saleId));
        tx.transition(saleId, SaleStatus.HELD, SaleStatus.OPEN);
        return view(saleId);
    }

    public SaleView voidSale(Long saleId, String reason) {
        assertCanModify(require(saleId));
        tx.voidSale(saleId, reason);
        return view(saleId);
    }

    // ------------------------------------------------------------------ checkout
    public SaleView checkout(Long saleId, CheckoutRequest request) {
        Sale sale = require(saleId);
        assertCanModify(sale);
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw ApiException.conflict("Sale is " + sale.getStatus() + " and cannot be checked out");
        }
        Shift shift = shifts.findById(sale.getShiftId());
        if (shift == null || shift.getStatus() != ShiftStatus.OPEN) {
            throw ApiException.conflict("The shift of this sale is closed");
        }
        Plan plan = TenderPlanner.plan(sale.getTotal(), request.payments());

        List<PaymentResult> approved = new ArrayList<>();
        try {
            for (Tender tender : plan.tenders()) {
                PaymentResult result = payments.charge(saleId, sale.getStoreId(), tender);
                if (!"APPROVED".equals(result.status())) {
                    throw new ApiException(HttpStatus.PAYMENT_REQUIRED, tender.method() + " payment was declined"
                            + (result.reason() == null ? "" : ": " + result.reason()));
                }
                approved.add(result);
            }
            tx.complete(saleId, plan, approved);
        } catch (RuntimeException e) {
            reverseAll(saleId, approved);
            throw e;
        }
        return view(saleId);
    }

    private void reverseAll(Long saleId, List<PaymentResult> approved) {
        for (PaymentResult result : approved) {
            try {
                payments.reverse(result.id());
            } catch (RuntimeException e) {
                log.error("MANUAL ACTION REQUIRED: could not reverse payment {} of sale {}: {}",
                        result.id(), saleId, e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ refund
    public SaleView refund(Long saleId, RefundRequest request) {
        Sale sale = require(saleId);
        CurrentUser.requireStoreAccess(sale.getStoreId());
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw ApiException.conflict("Only completed sales can be refunded (status " + sale.getStatus() + ")");
        }
        Map<Long, SaleLine> byId = new LinkedHashMap<>();
        for (SaleLine line : lineMapper.listBySale(saleId)) {
            byId.put(line.getId(), line);
        }

        List<RefundItem> items = new ArrayList<>();
        if (request.lines() == null || request.lines().isEmpty()) {
            for (SaleLine line : byId.values()) {
                BigDecimal remaining = line.getQuantity().subtract(line.getReturnedQty());
                if (remaining.signum() > 0) {
                    items.add(item(line, remaining));
                }
            }
        } else {
            Set<Long> seen = new HashSet<>();
            for (RefundLine requested : request.lines()) {
                SaleLine line = byId.get(requested.saleLineId());
                if (line == null) {
                    throw ApiException.badRequest("Line " + requested.saleLineId() + " does not belong to sale " + saleId);
                }
                if (!seen.add(requested.saleLineId())) {
                    throw ApiException.badRequest("Line " + requested.saleLineId() + " is listed twice");
                }
                BigDecimal remaining = line.getQuantity().subtract(line.getReturnedQty());
                if (requested.quantity().compareTo(remaining) > 0) {
                    throw ApiException.conflict("Only " + remaining.stripTrailingZeros().toPlainString()
                            + " of '" + line.getName() + "' can still be returned");
                }
                items.add(item(line, requested.quantity()));
            }
        }
        if (items.isEmpty()) {
            throw ApiException.conflict("Nothing left to refund on sale " + saleId);
        }
        BigDecimal refundTotal = items.stream().map(RefundItem::amount).reduce(Money.zero(), BigDecimal::add);
        if (refundTotal.signum() <= 0) {
            throw ApiException.badRequest("The refund amount is zero");
        }
        PaymentMethod method = request.method() == null ? PaymentMethod.CASH : request.method();
        boolean restock = request.restock() == null || request.restock();

        payments.refund(saleId, sale.getStoreId(), method, refundTotal, request.reason());
        Shift openShift = shifts.findOpenByTerminal(sale.getTerminalId());
        try {
            tx.recordRefund(saleId, openShift == null ? null : openShift.getId(), items, refundTotal, method,
                    request.reason(), restock, CurrentUser.get().userId());
        } catch (RuntimeException e) {
            log.error("MANUAL ACTION REQUIRED: refund of {} for sale {} was paid out but could not be recorded: {}",
                    refundTotal, saleId, e.getMessage());
            throw e;
        }
        return view(saleId);
    }

    /** The money credited for returned units is the proportional share of the (already net) line total. */
    private RefundItem item(SaleLine line, BigDecimal quantity) {
        BigDecimal amount = line.getLineTotal().multiply(quantity).divide(line.getQuantity(), 2, RoundingMode.HALF_UP);
        return new RefundItem(line, quantity, amount);
    }

    // ------------------------------------------------------------------ queries
    public SaleView view(Long saleId) {
        Sale sale = require(saleId);
        CurrentUser.requireStoreAccess(sale.getStoreId());
        return new SaleView(sale, lineMapper.listBySale(saleId), paymentMapper.listBySale(saleId),
                returns.listBySale(saleId));
    }

    public PageResult<Sale> search(Long storeId, String status, LocalDate from, LocalDate to, Integer page, Integer size) {
        Long store = CurrentUser.effectiveStoreId(storeId);
        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = SaleStatus.valueOf(status.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Unknown status '" + status + "'");
            }
        }
        ZoneId zone = ZoneId.of(timezone);
        Instant fromInstant = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();
        Paging paging = Paging.of(page, size);
        return PageResult.of(
                sales.search(store, normalizedStatus, fromInstant, toInstant, paging.size(), paging.offset()),
                paging,
                sales.count(store, normalizedStatus, fromInstant, toInstant));
    }

    // ------------------------------------------------------------------ helpers
    private Sale require(Long saleId) {
        Sale sale = sales.findById(saleId);
        if (sale == null) {
            throw ApiException.notFound("Sale", saleId);
        }
        return sale;
    }

    /** A cashier may change their own sales; supervisors and above may change anyone's in their store. */
    private void assertCanModify(Sale sale) {
        CurrentUser.requireStoreAccess(sale.getStoreId());
        AuthenticatedUser user = CurrentUser.get();
        if (!user.userId().equals(sale.getCashierId())
                && !user.hasAnyRole(Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR)) {
            throw ApiException.forbidden("This sale belongs to another cashier");
        }
    }

    /** Looks every product up in the catalog and snapshots price, discount and tax onto the lines.
     * Prices every line from the catalog: unit price, tax (the product's, or a supervisor's per-line override) and
     * the best applicable promotion for the quantity bought.
     */
    private List<SaleLine> priceLines(List<LineRequest> requests, Long storeId, List<String> couponCodes) {
        Set<String> coupons = couponCodes == null ? Set.of() : new HashSet<>(couponCodes);
        List<PromotionRule> rules = catalog.livePromotions();
        requireValidCoupons(coupons, rules, storeId);
        if (requests.stream().anyMatch(r -> r.tax() != null)) {
            CurrentUser.requireAnyRole(Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR);
        }
        Instant now = Instant.now();
        Map<Long, ProductInfo> cache = new HashMap<>();
        List<SaleLine> lines = new ArrayList<>();
        for (LineRequest request : requests) {
            ProductInfo product = cache.computeIfAbsent(request.productId(), catalog::getProduct);
            if (!product.active()) {
                throw ApiException.conflict("Product '" + product.name() + "' is not for sale");
            }
            LineTax override = request.tax();
            BigDecimal taxRate = override != null ? override.rate()
                    : product.taxRate() == null ? BigDecimal.ZERO : product.taxRate();
            boolean inclusive = override != null && override.inclusive() != null ? override.inclusive()
                    : product.taxInclusive();
            String taxCode = override != null ? override.code()
                    : product.tax() == null ? null : product.tax().code();

            LineDiscount promo = PromotionEngine.lineDiscount(product.price(), request.quantity(), product.id(),
                    product.categoryId(), storeId, rules, coupons, now);
            LineAmounts amounts = PricingCalculator.lineWithDiscount(request.quantity(), product.price(), promo.total(),
                    taxRate, inclusive);

            SaleLine line = new SaleLine();
            line.setProductId(product.id());
            line.setSku(product.sku());
            line.setName(product.name());
            line.setQuantity(request.quantity());
            line.setUnitPrice(product.price());
            line.setDiscountPerUnit(amounts.discountTotal().divide(request.quantity(), 2, RoundingMode.HALF_UP));
            line.setDiscountTotal(amounts.discountTotal());
            line.setPromotionCode(amounts.discountTotal().signum() > 0 ? promo.promotionCode() : null);
            line.setTaxCode(taxCode);
            line.setTaxOverridden(override != null);
            line.setTaxRate(taxRate);
            line.setTaxInclusive(inclusive);
            line.setTaxAmount(amounts.taxAmount());
            line.setLineTotal(amounts.lineTotal());
            lines.add(line);
        }
        return lines;
    }

    /** A coupon the cashier typed must exist, be live and be valid in this store; otherwise tell them right away. */
    private static void requireValidCoupons(Set<String> coupons, List<PromotionRule> rules, Long storeId) {
        for (String coupon : coupons) {
            boolean known = rules.stream().anyMatch(r -> r.couponCode() != null
                    && r.couponCode().trim().equalsIgnoreCase(coupon.trim())
                    && (r.storeId() == null || r.storeId().equals(storeId)));
            if (!known) {
                throw ApiException.badRequest("Coupon '" + coupon.trim() + "' is not valid");
            }
        }
    }
}
