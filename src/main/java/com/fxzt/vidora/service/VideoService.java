package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.CommentPageDto;
import com.fxzt.vidora.pojo.query.VideoCreateDTo;
import com.fxzt.vidora.pojo.query.VideoMyPageDto;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import com.fxzt.vidora.pojo.vo.*;

public interface VideoService {
    /**
     * 添加视频
     */
    void addVideo(VideoCreateDTo videoCreateDTo);

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
}
