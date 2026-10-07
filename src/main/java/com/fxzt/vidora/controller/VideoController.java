package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.pojo.query.*;
import com.fxzt.vidora.pojo.vo.*;
import com.fxzt.vidora.service.VideoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/video")
public class VideoController {

    @Autowired
    private VideoService videoService;

    /**
     * 根据id获取视频
     */
    @GetMapping("/{id}")
    public Result<VideoVo> getVideoById(@PathVariable Long id){
        log.debug("根据id获取视频，id{}", id);
        VideoVo videoVo = videoService.getVideoById(id);
        return Result.success(videoVo);
    }

    /**
     * 获取视频列表
     */
    @GetMapping
    public Result<PageResult<VideoListItem>> pageVideo(@Valid VideoPageDTo videoPageDTo){
        log.debug("获取视频列表，查询参数{}", videoPageDTo);
        PageResult<VideoListItem> videoList = videoService.pageVideo(videoPageDTo);
        return Result.success(videoList);
    }

    /**
     * 获取个人投稿视频列表
     */
    @GetMapping("/mine")
    public Result<PageResult<VideoListItem>> pageMyVideo(@Valid VideoMyPageDto videoPageDTo){
        log.debug("获取个人投稿视频列表，查询参数{}", videoPageDTo);
        PageResult<VideoListItem> videoList = videoService.pageMyVideo(videoPageDTo);
        return Result.success(videoList);
    }

    /**
     * 点赞视频
     */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id){
        log.debug("点赞视频，id{}", id);
        videoService.videoLike(id);
        return Result.success();
    }

    /**
     * 取消点赞视频
     */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id){
        log.debug("取消点赞视频，id{}", id);
        videoService.unlike(id);
        return Result.success();
    }

    /**
     * 添加视频评论
     */
    @PostMapping("/{id}/comment")
    public Result<Void> addComment(@PathVariable Long id,@Valid @RequestBody CommentCreateDto dto){
        log.debug("添加视频评论，视频id{},评论{}", id, dto.getContent());
        videoService.addComment(id, dto.getContent());
        return Result.success();
    }

    /**
     * 获取视频评论列表
     */
    @GetMapping("/{id}/comment")
    public Result<PageCommentResult<CommentVo>> commentList(@PathVariable Long id,@Valid CommentPageDto commentPageDto){
        log.debug("获取视频评论，视频id{}", id);
        PageCommentResult<CommentVo> commentList = videoService.commentList(id, commentPageDto);
        return Result.success(commentList);
    }

    /**
     * 删除视频评论
     */
    @DeleteMapping("/{id}/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId){
        log.debug("删除视频评论，视频id{},评论id{}", id, commentId);
        videoService.deleteComment(id, commentId);
        return Result.success();
    }

    @GetMapping("/feed")
    public Result<Object> feed(@Valid FeedPageDto dto){
        log.debug("获取首页推荐流，参数{}", dto);
        return "hot".equals(dto.getSort())
                ? Result.success(videoService.hotFeed(dto))
                : Result.success(videoService.feedPage(dto));
    }

    /**
     * 收藏视频
     */
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id){
        log.debug("收藏视频，id{}", id);
        videoService.videoFavorite(id);
        return Result.success();
    }

    /**
     * 取消收藏视频
     */
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id){
        log.debug("取消收藏视频，id{}", id);
        videoService.unfavorite(id);
        return Result.success();
    }
}
