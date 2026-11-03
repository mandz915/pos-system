package com.pos.sales.service;

import java.time.LocalDate;
import java.util.List;

import com.pos.common.dto.PageResult;
import com.pos.sales.dto.Sale;
import com.pos.sales.dto.SalesDtos.CheckoutRequest;
import com.pos.sales.dto.SalesDtos.CreateSaleRequest;
import com.pos.sales.dto.SalesDtos.LineRequest;
import com.pos.sales.dto.SalesDtos.RefundRequest;
import com.pos.sales.dto.SalesDtos.SaleView;

public interface SaleService {

    SaleView create(CreateSaleRequest request);

    SaleView replaceLines(Long saleId, List<LineRequest> requests, List<String> couponCodes);

    SaleView hold(Long saleId);

    SaleView resume(Long saleId);

    SaleView voidSale(Long saleId, String reason);

    SaleView checkout(Long saleId, CheckoutRequest request);

    SaleView refund(Long saleId, RefundRequest request);

    SaleView view(Long saleId);

    PageResult<Sale> search(Long storeId, String status, LocalDate from, LocalDate to, Integer page, Integer size);
}
