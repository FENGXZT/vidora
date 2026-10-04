package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.*;
import com.fxzt.vidora.pojo.vo.*;
import com.fxzt.vidora.service.VideoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/video")
public class VideoController {

    @Autowired
    private VideoService videoService;

    /**
     * 添加视频
     */
    @PostMapping
    public Result<Void> addVideo(@Valid @RequestBody VideoCreateDTo videoCreateDTo) {
        log.info("添加视频，添加视频信息{}", videoCreateDTo);
        videoService.addVideo(videoCreateDTo);
        return Result.success();
    }

    /**
     * 根据id获取视频
     */
    @GetMapping("/{id}")
    public Result<VideoVo> getVideoById(@PathVariable Long id){
        log.info("根据id获取视频，id{}", id);
        VideoVo videoVo = videoService.getVideoById(id);
        return Result.success(videoVo);
    }

    /**
     * 获取视频列表
     */
    @GetMapping
    public Result<PageResult<VideoListItem>> pageVideo(@Valid VideoPageDTo videoPageDTo){
        log.info("获取视频列表，查询参数{}", videoPageDTo);
        PageResult<VideoListItem> videoList = videoService.pageVideo(videoPageDTo);
        return Result.success(videoList);
    }

    /**
     * 获取个人投稿视频列表
     */
    @GetMapping("/mine")
    public Result<PageResult<VideoListItem>> pageMyVideo(@Valid VideoMyPageDto videoPageDTo){
        log.info("获取个人投稿视频列表，查询参数{}", videoPageDTo);
        PageResult<VideoListItem> videoList = videoService.pageMyVideo(videoPageDTo);
        return Result.success(videoList);
    }

    /**
     * 点赞视频
     */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id){
        log.info("点赞视频，id{}", id);
        videoService.videoLike(id);
        return Result.success();
    }

    /**
     * 取消点赞视频
     */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id){
        log.info("取消点赞视频，id{}", id);
        videoService.unlike(id);
        return Result.success();
    }

    /**
     * 添加视频评论
     */
    @PostMapping("/{id}/comment")
    public Result<Void> addComment(@PathVariable Long id,@Valid @RequestBody CommentCreateDto dto){
        log.info("添加视频评论，视频id{},评论{}", id, dto.getContent());
        videoService.addComment(id, dto.getContent());
        return Result.success();
    }

    /**
     * 获取视频评论列表
     */
    @GetMapping("/{id}/comment")
    public Result<PageCommentResult<CommentVo>> commentList(@PathVariable Long id,@Valid CommentPageDto commentPageDto){
        log.info("获取视频评论，视频id{}", id);
        PageCommentResult<CommentVo> commentList = videoService.commentList(id, commentPageDto);
        return Result.success(commentList);
    }

    /**
     * 删除视频评论
     */
    @DeleteMapping("/{id}/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId){
        log.info("删除视频评论，视频id{},评论id{}", id, commentId);
        videoService.deleteComment(id, commentId);
        return Result.success();
    }
}
