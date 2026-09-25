package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoCreateDTo;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import com.fxzt.vidora.pojo.vo.PageResult;
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
    public Result<Video> getVideoById(@PathVariable Long id){
        log.info("根据id获取视频，id{}", id);
        Video video = videoService.getVideoById(id);
        return Result.success(video);
    }

    /**
     * 获取视频列表
     */
    @GetMapping
    public Result<PageResult<Video>> pageVideo(VideoPageDTo videoPageDTo){
        log.info("获取视频列表，查询参数{}", videoPageDTo);
        PageResult<Video> videoList = videoService.pageVideo(videoPageDTo);
        return Result.success(videoList);
    }
}
