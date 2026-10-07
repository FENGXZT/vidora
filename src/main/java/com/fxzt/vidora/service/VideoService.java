package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.query.*;
import com.fxzt.vidora.pojo.vo.*;
import jakarta.validation.Valid;

public interface VideoService {
    /**
     * 根据id获取视频
     */
    VideoVo getVideoById(Long id);

    /**
     * 分页获取视频列表
     */
    PageResult<VideoListItem> pageVideo(VideoPageDTo videoPageDTo);

    /**
     * 分页获取个人投稿视频列表
     */
    PageResult<VideoListItem> pageMyVideo(VideoMyPageDto videoPageDTo);

    /**
     * 点赞视频
     */
    void videoLike(Long id);

    /**
     * 取消点赞视频
     */
    void unlike(Long id);

    /**
     * 添加视频评论
     */
    void addComment(Long id, String comment);

    /**
     * 获取视频评论列表
     */
    PageCommentResult<CommentVo> commentList(Long id, CommentPageDto commentPageDto);

    /**
     * 删除视频评论
     */
    void deleteComment(Long id, Long commentId);

    /**
     * 获取推荐视频列表
     */
    CursorPage<VideoListItem> feedPage(FeedPageDto feedPageDto);

    /**
     * 获取热门视频列表
     */
    ScoreCursorPageWithId<VideoListItem> hotFeed(FeedPageDto dto);

    /**
     * 收藏视频
     */
    void videoFavorite(Long id);

    /**
     * 取消收藏视频
     */
    void unfavorite(Long id);
}
