package com.fxzt.vidora.mapper;

import com.fxzt.vidora.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     */
    @Select("select * from user where username = #{username}")
    User findByUsername(String username);

    /**
     * 插入用户
     * @param user 用户
     */
    void insert(User user);
}
