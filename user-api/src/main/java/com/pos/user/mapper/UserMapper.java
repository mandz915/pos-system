package com.pos.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.user.domain.User;

@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO users (username, password_hash, pin_hash, full_name, email, store_id, active)
            VALUES (#{username}, #{passwordHash}, #{pinHash}, #{fullName}, #{email}, #{storeId}, #{active})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);

    @Select("SELECT * FROM users WHERE lower(username) = lower(#{username})")
    User findByUsername(@Param("username") String username);

    @Select("""
            <script>
            SELECT * FROM users
            <where>
              <if test="q != null">(username ILIKE #{q} OR full_name ILIKE #{q})</if>
            </where>
            ORDER BY id LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<User> search(@Param("q") String q, @Param("size") int size, @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM users
            <where>
              <if test="q != null">(username ILIKE #{q} OR full_name ILIKE #{q})</if>
            </where>
            </script>
            """)
    long count(@Param("q") String q);

    @Select("SELECT count(*) FROM users")
    long countAll();

    @Update("UPDATE users SET full_name = #{fullName}, email = #{email}, store_id = #{storeId}, updated_at = now() WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE users SET active = #{active}, updated_at = now() WHERE id = #{id}")
    int setActive(@Param("id") Long id, @Param("active") boolean active);

    @Update("UPDATE users SET password_hash = #{hash}, updated_at = now() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("hash") String hash);

    @Update("UPDATE users SET pin_hash = #{hash}, updated_at = now() WHERE id = #{id}")
    int updatePin(@Param("id") Long id, @Param("hash") String hash);

    @Select("""
            SELECT r.name FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.name
            """)
    List<String> rolesOf(@Param("userId") Long userId);

    @Select("SELECT name FROM roles ORDER BY id")
    List<String> allRoles();

    @Insert("""
            INSERT INTO user_roles (user_id, role_id)
            SELECT #{userId}, id FROM roles WHERE name = #{role}
            ON CONFLICT DO NOTHING
            """)
    int addRole(@Param("userId") Long userId, @Param("role") String role);

    @Delete("DELETE FROM user_roles WHERE user_id = #{userId}")
    int clearRoles(@Param("userId") Long userId);
}
