package com.pos.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.user.domain.User;

@Mapper
public interface UserMapper {

    int insert(User user);

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    List<User> search(@Param("q") String q, @Param("size") int size, @Param("offset") int offset);

    long count(@Param("q") String q);

    long countAll();

    List<Long> allIds();

    void update(User user);

    void setActive(@Param("id") Long id, @Param("active") boolean active);

    void updatePassword(@Param("id") Long id, @Param("hash") String hash);

    void updatePin(@Param("id") Long id, @Param("hash") String hash);

    List<String> rolesOf(@Param("userId") Long userId);

    List<String> allRoles();

    void addRole(@Param("userId") Long userId, @Param("role") String role);

    void clearRoles(@Param("userId") Long userId);
}
