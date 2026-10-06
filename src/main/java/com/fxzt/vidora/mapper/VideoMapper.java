package com.fxzt.vidora.mapper;

import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoCacheDto;
import com.fxzt.vidora.pojo.query.VideoMyPageDto;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VideoMapper {
    /**
     * 添加视频
     */
    void addVideo(Video video);

    /**
     * 根据id获取视频
     */
    Video getVideoById(Long id);

    /**
     * 分页查询视频
     */
    List<VideoCacheDto> findList(VideoPageDTo videoPageDTo);

    /**
     * 根据id获取视频详细
     */
    @Select("select * from video where id = #{id}")
    Video findById(Long id);

    /**
     * 更新视频
     */
    void updateVideo(Video video);

    /**
     * 根据id删除视频
     */
    @Delete("delete from video where id = #{id}  and status = 0")
    void deleteById(Long id);

    /**
     * 获取清理列表
     */
    @Select("select * from video where status = #{status} and create_time < #{time}")
    List<Video> cleanList(@Param("status") int status, @Param("time") LocalDateTime time);

    /**
     * 根据文件哈希值获取已存在的视频
     */
    @Select("select * from video where file_hash = #{fileHash} and status = 2 limit 1")
    Video findReusableByHash(String fileHash);

    /**
     * 根据文件哈希值和id,用户上传中断的视频
     */
    @Select("select * from video where file_hash = #{fileHash} and status = 0 and user_id = #{userId} limit 1")
    Video findResumableByHash(@Param("fileHash") String fileHash,@Param("userId") Long userId);

    /**
     * 查询自己的视频
     */
    List<VideoCacheDto> findMyList(@Param("dto") VideoMyPageDto videoPageDTo, @Param("userId") Long userId);

    /**
     * 更新视频播放量
     */
    @Update("update video set view_count = #{count} where id = #{videoId} and view_count != #{count}")
    void updateViewCount(@Param("videoId") Long videoId,@Param("count") Integer count);

    /**
     * 根据id获取视频播放量
     */
    @Select("select view_count from video where id = #{id}")
    Long findViewCountById(Long id);

    /**
     * 更新视频点赞量
     */
    @Update("update video set like_count = #{likeCount} where id = #{videoId}")
    void updateLikeCount(@Param("videoId") Long videoId,@Param("likeCount") Integer likeCount);

    /**
     * 更新视频点赞量
     */
    void updateLikeCountFullAmount();

    /**
     * 获取推荐视频列表
     */
    List<VideoCacheDto> findFeed(@Param("cursorTime") LocalDateTime cursorTime,@Param("cursorId") Long cursorId,@Param("size") int size);

    /**
     * 获取用户点赞视频列表
     */
    List<VideoCacheDto> findLikedByUser(@Param("cursorTime") LocalDateTime cursorTime,@Param("cursorId") Long cursorId
            ,@Param("size") int size,@Param("userId") Long userId);

    /**
     * 获取用户收藏视频列表
     */
    List<VideoCacheDto> findFavoritesByUser(@Param("cursorTime") LocalDateTime cursorTime,@Param("cursorId") Long cursorId
            ,@Param("size") int size,@Param("userId") Long userId);

    /**
     * 更新视频收藏量
     */
    @Update("update video set favorite_count = #{favoriteCount} where id = #{videoId}")
    void updateFavoriteCount(@Param("videoId") Long videoId,@Param("favoriteCount") Integer favoriteCount);

    /**
     * 更新视频点赞量
     */
    void updateFavoriteCountFullAmount();

    List<VideoCacheDto> findByIds(@Param("ids") List<Long> ids);
}
