package com.pos.customer.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.customer.domain.Customer;

@Mapper
public interface CustomerMapper {

    @Insert("""
            INSERT INTO customers (code, name, phone, email, address, credit_limit, active)
            VALUES (#{code}, #{name}, #{phone}, #{email}, #{address}, #{creditLimit}, #{active})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Customer customer);

    @Select("SELECT * FROM customers WHERE id = #{id}")
    Customer findById(@Param("id") Long id);

    @Select("SELECT * FROM customers WHERE id = #{id} FOR UPDATE")
    Customer lock(@Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM customers
            <where>
              <if test="q != null">AND (name ILIKE #{q} OR code ILIKE #{q} OR phone ILIKE #{q} OR email ILIKE #{q})</if>
            </where>
            ORDER BY name LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Customer> search(@Param("q") String q, @Param("size") int size, @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM customers
            <where>
              <if test="q != null">AND (name ILIKE #{q} OR code ILIKE #{q} OR phone ILIKE #{q} OR email ILIKE #{q})</if>
            </where>
            </script>
            """)
    long count(@Param("q") String q);

    @Update("UPDATE customers SET name = #{name}, phone = #{phone}, email = #{email}, address = #{address}, credit_limit = #{creditLimit} WHERE id = #{id}")
    int update(Customer customer);

    @Update("UPDATE customers SET active = #{active} WHERE id = #{id}")
    int setActive(@Param("id") Long id, @Param("active") boolean active);

    @Update("UPDATE customers SET points_balance = #{points} WHERE id = #{id}")
    int setPoints(@Param("id") Long id, @Param("points") long points);

    @Update("UPDATE customers SET credit_balance = #{balance} WHERE id = #{id}")
    int setCreditBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}
