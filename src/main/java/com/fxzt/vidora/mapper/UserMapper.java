package com.fxzt.vidora.mapper;

import com.fxzt.vidora.pojo.entity.User;
import com.fxzt.vidora.pojo.query.UserCacheDto;
import com.fxzt.vidora.pojo.query.UserRegisterDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

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
    void insert(UserRegisterDto user);

    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     */
    @Select("Select * from user where id = #{userId}")
    User findById(Long userId);

    /**
     * 更新用户
     * @param user 用户
     */
    void update(User user);

    /**
     * 根据用户ID查询关注的用户
     * @param cursorTime 时间游标
     * @param cursorId ID游标
     * @param size 大小
     * @param userId 用户ID
     */
    List<UserCacheDto> findFollowingByUser(@Param("cursorTime") LocalDateTime cursorTime, @Param("cursorId") Long cursorId, @Param("size") int size, @Param("userId") Long userId);
}
