package com.pos.customer.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.customer.domain.Customer;

@Mapper
public interface CustomerMapper {

    void insert(Customer customer);

    Customer findById(@Param("id") Long id);

    Customer lock(@Param("id") Long id);

    List<Customer> search(@Param("q") String q, @Param("size") int size, @Param("offset") int offset);

    long count(@Param("q") String q);

    void update(Customer customer);

    void setActive(@Param("id") Long id, @Param("active") boolean active);

    void setPoints(@Param("id") Long id, @Param("points") long points);

    void setCreditBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}
