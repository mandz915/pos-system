package com.pos.customer.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.customer.dto.Customer;

@Mapper
public interface CustomerMapper {

    int insert(Customer customer);

    Customer findById(@Param("id") Long id);

    Customer lock(@Param("id") Long id);

    List<Customer> search(@Param("q") String q, @Param("size") int size, @Param("offset") int offset);

    long count(@Param("q") String q);

    int update(Customer customer);

    int setActive(@Param("id") Long id, @Param("active") boolean active);

    int setPoints(@Param("id") Long id, @Param("points") long points);

    int setCreditBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}
