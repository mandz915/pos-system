package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.administration.domain.Terminal;

@Mapper
public interface TerminalMapper {

    @Insert("INSERT INTO terminals (store_id, code, name, active) VALUES (#{storeId}, #{code}, #{name}, #{active})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Terminal terminal);

    @Select("SELECT * FROM terminals WHERE id = #{id}")
    Terminal findById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM terminals
            <where>
              <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            ORDER BY code
            </script>
            """)
    List<Terminal> findAll(@Param("storeId") Long storeId);

    @Update("UPDATE terminals SET store_id = #{storeId}, code = #{code}, name = #{name} WHERE id = #{id}")
    int update(Terminal terminal);

    @Update("UPDATE terminals SET active = #{active} WHERE id = #{id}")
    int setActive(@Param("id") Long id, @Param("active") boolean active);

    @Update("UPDATE terminals SET last_seen_at = now() WHERE id = #{id}")
    int heartbeat(@Param("id") Long id);
}
