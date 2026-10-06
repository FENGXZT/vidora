package com.fxzt.vidora.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface VideoFavoriteMapper {
    /**
     * 添加收藏
     * @param id 视频id
     * @param userId 用户id
     */
    @Insert("insert ignore into video_favorite (video_id, user_id) values (#{id}, #{userId}) on Duplicate Key Update id = id")
    void addFavorite(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 取消收藏
     * @param id 视频id
     * @param userId 用户id
     */
    @Delete("delete from video_favorite where video_id = #{id} and user_id = #{userId}")
    void unFavorite(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 获取收藏数
     * @param id 视频id
     * @return 收藏数
     */
    @Select("select count(*) from video_favorite where video_id = #{id}")
    Integer getFavoriteCount(Long id);

    /**
     * 判断用户是否收藏
     * @param id 视频id
     * @param userId 用户id
     * @return 收藏数
     */
    @Select("select count(*) from video_favorite where video_id = #{id} and user_id = #{userId}")
    Integer isFavorited(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据视频id删除
     * @param id 视频id
     */
    @Delete("delete from video_favorite where video_id = #{id}")
    void deleteByVideoId(Long id);
}
