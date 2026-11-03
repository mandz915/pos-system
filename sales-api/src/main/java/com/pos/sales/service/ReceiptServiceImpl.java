package com.pos.sales.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pos.common.error.ApiException;
import com.pos.sales.dto.Sale;
import com.pos.sales.dto.SaleLine;
import com.pos.sales.dto.SalePayment;
import com.pos.sales.dto.SaleStatus;
import com.pos.sales.dto.SalesDtos.SaleView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final int WIDTH = 40;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SaleService sales;

    @Value("${pos.receipt.header:POS System}")
    private String header;

    @Value("${pos.receipt.footer:Thank you!}")
    private String footer;

    @Value("${pos.receipt.timezone:Asia/Manila}")
    private String timezone;

    @Override
    public String render(Long saleId) {
        SaleView view = sales.view(saleId);
        Sale sale = view.sale();
        if (sale.getStatus() != SaleStatus.COMPLETED && sale.getStatus() != SaleStatus.REFUNDED) {
            throw ApiException.conflict("A receipt is only available for completed sales");
        }
        StringBuilder out = new StringBuilder();
        line(out, center(header));
        line(out, rule());
        line(out, row("Receipt", sale.getReceiptNo()));
        line(out, row("Date", DATE_TIME.format(sale.getCompletedAt().atZone(ZoneId.of(timezone)))));
        line(out, row("Cashier", "#" + sale.getCashierId()));
        line(out, row("Terminal", "#" + sale.getTerminalId()));
        line(out, rule());
        for (SaleLine item : view.lines()) {
            line(out, item.getName());
            String detail = "  " + item.getQuantity().stripTrailingZeros().toPlainString() + " x " + item.getUnitPrice();
            line(out, row(detail, item.getLineTotal().toPlainString()));
            if (item.getDiscountTotal().signum() > 0) {
                line(out, row("  discount", "-" + item.getDiscountTotal().toPlainString()));
            }
        }
        line(out, rule());
        line(out, row("Subtotal", sale.getSubtotal().toPlainString()));
        if (sale.getDiscountTotal().signum() > 0) {
            line(out, row("Discount", "-" + sale.getDiscountTotal().toPlainString()));
        }
        line(out, row("Tax", sale.getTaxTotal().toPlainString()));
        line(out, row("TOTAL", sale.getTotal().toPlainString()));
        line(out, rule());
        for (SalePayment payment : view.payments()) {
            line(out, row(payment.getMethod().name(), payment.getTendered().toPlainString()));
        }
        line(out, row("Change", sale.getChangeDue().toPlainString()));
        line(out, rule());
        line(out, center(footer));
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String rule() {
        return "-".repeat(WIDTH);
    }

    private static String center(String text) {
        if (text.length() >= WIDTH) {
            return text.substring(0, WIDTH);
        }
        return " ".repeat((WIDTH - text.length()) / 2) + text;
    }

    private static String row(String left, String right) {
        int available = Math.max(1, WIDTH - right.length() - 1);
        String clipped = left.length() > available ? left.substring(0, available) : left;
        return clipped + " ".repeat(WIDTH - clipped.length() - right.length()) + right;
    }
}
