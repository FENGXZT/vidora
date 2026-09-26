package com.fxzt.vidora.mapper;

import com.fxzt.vidora.pojo.entity.Video;
import com.fxzt.vidora.pojo.query.VideoCacheDto;
import com.fxzt.vidora.pojo.query.VideoPageDTo;
import org.apache.ibatis.annotations.Mapper;

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
}
