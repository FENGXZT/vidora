package com.fxzt.vidora.service;

import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoCreateDTo;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import com.fxzt.vidora.pojo.vo.PageResult;
import com.fxzt.vidora.pojo.vo.VideoListItem;
import com.fxzt.vidora.pojo.vo.VideoVo;

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
}
