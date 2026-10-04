package com.fxzt.vidora.mapper;

import com.fxzt.vidora.pojo.query.CommentCacheDto;
import com.fxzt.vidora.pojo.query.CommentPageDto;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommentMapper {
    /**
     * 添加评论
     * @param id 视频id
     * @param userId 用户id
     * @param content 评论内容
     */
    @Insert("insert into comment (video_id, user_id, content) values (#{id}, #{userId}, #{content})")
    void addComment(@Param("id") Long id, @Param("userId") Long userId, @Param("content") String content);

    /**
     * 获取评论列表
     * @param id 视频id
     * @param cursorTime 游标时间
     * @param cursorId 游标id
     * @param size 分页大小
     * @return 评论列表
     */
    List<CommentCacheDto> commentList(@Param("id") Long id, @Param("cursorTime") LocalDateTime cursorTime, @Param("cursorId") Long cursorId, @Param("size") Integer size);

    /**
     * 删除评论
     * @param commentId 评论id
     */
    @Delete("delete from comment where id = #{commentId}")
    void deleteComment(Long commentId);

    @Select("select user_id from comment where id = #{commentId} and video_id = #{id}")
    Long findUserIdById(@Param("commentId") Long commentId,@Param("id") Long id);
}
