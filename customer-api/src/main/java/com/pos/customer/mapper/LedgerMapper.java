package com.pos.customer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.customer.domain.CreditTransaction;
import com.pos.customer.domain.LoyaltyTransaction;

@Mapper
public interface LedgerMapper {

    @Insert("""
            INSERT INTO loyalty_transactions (customer_id, type, points, balance, ref_type, ref_id, note)
            VALUES (#{customerId}, #{type}, #{points}, #{balance}, #{refType}, #{refId}, #{note})
            """)
    int insertLoyalty(LoyaltyTransaction transaction);

    @Select("SELECT * FROM loyalty_transactions WHERE customer_id = #{customerId} ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<LoyaltyTransaction> loyaltyHistory(@Param("customerId") Long customerId, @Param("size") int size, @Param("offset") int offset);

    @Select("SELECT count(*) FROM loyalty_transactions WHERE customer_id = #{customerId}")
    long loyaltyCount(@Param("customerId") Long customerId);

    @Insert("""
            INSERT INTO credit_transactions (customer_id, type, amount, balance, ref_type, ref_id, note)
            VALUES (#{customerId}, #{type}, #{amount}, #{balance}, #{refType}, #{refId}, #{note})
            """)
    int insertCredit(CreditTransaction transaction);

    @Select("SELECT * FROM credit_transactions WHERE customer_id = #{customerId} ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<CreditTransaction> creditHistory(@Param("customerId") Long customerId, @Param("size") int size, @Param("offset") int offset);

    @Select("SELECT count(*) FROM credit_transactions WHERE customer_id = #{customerId}")
    long creditCount(@Param("customerId") Long customerId);

    /** @return 1 when the event is new, 0 when it was already processed. */
    @Insert("INSERT INTO processed_events (event_id) VALUES (#{eventId}) ON CONFLICT DO NOTHING")
    int markProcessed(@Param("eventId") String eventId);
}
