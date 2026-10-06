package com.fxzt.vidora.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFollowMapper {

    @Insert("insert into user_follow (follower_id, followee_id) values (#{me}, #{id}) on duplicate key update id = id")
    void addFollow(@Param("me") Long me, @Param("id") Long id);

    @Delete("delete from user_follow where follower_id = #{me} and followee_id = #{id}")
    void unfollow(@Param("me") Long me, @Param("id") Long id);
}
