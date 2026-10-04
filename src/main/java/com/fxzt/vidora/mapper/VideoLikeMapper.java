package com.fxzt.vidora.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface VideoLikeMapper {

    /**
     * 添加点赞
     * @param id 视频id
     * @param userId 用户id
     */
    @Insert("insert ignore into video_like (video_id, user_id) values (#{id}, #{userId}) on Duplicate Key Update id = id")
    void addLike(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 取消点赞
     * @param id 视频id
     * @param userId 用户id
     */
    @Delete("delete from video_like where video_id = #{id} and user_id = #{userId}")
    void unlike(Long id, Long userId);

    /**
     * 获取点赞数
     * @param id 视频id
     * @return 点赞数
     */
    @Select("select count(*) from video_like where video_id = #{id}")
    Integer getLikeCount(Long id);

    /**
     * 判断用户是否点赞
     * @param id 视频id
     * @param userId 用户id
     * @return 点赞数
     */
    @Select("select count(*) from video_like where video_id = #{id} and user_id = #{userId}")
    Integer isLiked(@Param("id") Long id, @Param("userId") Long userId);
}
