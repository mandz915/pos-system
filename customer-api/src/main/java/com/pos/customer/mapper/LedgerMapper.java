package com.pos.customer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.customer.dto.CreditTransaction;
import com.pos.customer.dto.LoyaltyTransaction;

@Mapper
public interface LedgerMapper {

    int insertLoyalty(LoyaltyTransaction transaction);

    List<LoyaltyTransaction> loyaltyHistory(@Param("customerId") Long customerId, @Param("size") int size, @Param("offset") int offset);

    long loyaltyCount(@Param("customerId") Long customerId);

    int insertCredit(CreditTransaction transaction);

    List<CreditTransaction> creditHistory(@Param("customerId") Long customerId, @Param("size") int size, @Param("offset") int offset);

    long creditCount(@Param("customerId") Long customerId);

    int markProcessed(@Param("eventId") String eventId);
}
