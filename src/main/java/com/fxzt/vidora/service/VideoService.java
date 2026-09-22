package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import com.fxzt.vidora.pojo.vo.PageResult;

import java.util.List;

public interface VideoService {
    /**
     * 添加视频
     */
    void addVideo(Video video);

    /**
     * 根据id获取视频
     */
    Video getVideoById(Long id);

    /**
     * 分页获取视频列表
     */
    PageResult<Video> pageVideo(VideoPageDTo videoPageDTo);
}
